package com.social.ripple.external_ingestion.implementation;

import java.io.IOException;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.social.ripple.external_ingestion.dao.model.ConfigParameter;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.ConfigKeys;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FetchedMediaStorageService {

	private static final String DEFAULT_BUNNY_STORAGE_HOST = "storage.bunnycdn.com";

	private final RestTemplate restTemplate;

	public StoredFetchedMedia storeExternalMedia(String traceId, String externalUrl, String mediaType, Long organizationId) throws IOException {
		return storeExternalMedia(traceId, externalUrl, mediaType, organizationId, Collections.emptyMap());
	}

	public StoredFetchedMedia storeExternalMedia(String traceId, String externalUrl, String mediaType, Long organizationId,
			Map<String, String> additionalDownloadHeaders) throws IOException {
		if (!StringUtils.hasText(externalUrl)) {
			throw new IOException("External media URL is empty");
		}

		String normalizedMediaType = normalizeMediaType(mediaType);
		enforceBunnyStorageConfigured(traceId, normalizedMediaType);

		HttpHeaders downloadHeaders = new HttpHeaders();
		downloadHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; SocialRippleFetcher/1.0)");
		if (additionalDownloadHeaders != null) {
			additionalDownloadHeaders.forEach((name, value) -> {
				if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
					downloadHeaders.set(name, value);
				}
			});
		}
		ResponseEntity<byte[]> downloadResponse = restTemplate.exchange(externalUrl, HttpMethod.GET, new HttpEntity<>(downloadHeaders), byte[].class);
		if (!downloadResponse.getStatusCode().is2xxSuccessful() || downloadResponse.getBody() == null || downloadResponse.getBody().length == 0) {
			throw new IOException("Failed to download external media: " + externalUrl + " status=" + downloadResponse.getStatusCode());
		}

		String zone = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_ZONE);
		String accessKey = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_ACCESS_KEY);
		String pullZoneBaseUrl = trimTrailingSlash(getConfig(traceId, ConfigKeys.BUNNY_PULL_ZONE_BASE_URL));
		String storageHost = resolveStorageHost(traceId);

		String extension = resolveExtension(externalUrl, downloadResponse.getHeaders().getContentType(), normalizedMediaType);
		String objectKey = buildObjectKey(organizationId, normalizedMediaType, extension);
		String uploadUrl = "https://" + storageHost + "/" + zone + "/" + objectKey;
		MediaType uploadContentType = resolveContentType(downloadResponse.getHeaders().getContentType(), extension, normalizedMediaType);

		HttpHeaders uploadHeaders = new HttpHeaders();
		uploadHeaders.set("AccessKey", accessKey);
		uploadHeaders.setContentType(uploadContentType);
		ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.PUT, new HttpEntity<>(downloadResponse.getBody(), uploadHeaders),
				String.class);
		if (!uploadResponse.getStatusCode().is2xxSuccessful() && !uploadResponse.getStatusCode().is3xxRedirection()) {
			throw new IOException("Bunny upload failed with status " + uploadResponse.getStatusCode());
		}

		String fileUrl = pullZoneBaseUrl + "/" + objectKey;
		log.info("[{}]|FETCHED_MEDIA|BUNNY_STORED|type={} url={}", traceId, normalizedMediaType, fileUrl);

		return StoredFetchedMedia.builder()
				.fileUrl(fileUrl)
				.playbackUrl("VIDEO".equals(normalizedMediaType) ? fileUrl : null)
				.thumbnailUrl(null)
				.storageProvider("BUNNY_STORAGE")
				.processingStatus("READY")
				.mediaType(normalizedMediaType)
				.build();
	}

	private void enforceBunnyStorageConfigured(String traceId, String mediaType) {
		String providerKey = "VIDEO".equals(mediaType) ? ConfigKeys.MEDIA_VIDEO_PROVIDER : ConfigKeys.MEDIA_IMAGE_PROVIDER;
		String provider = getConfig(traceId, providerKey);
		if (!"BUNNY_STORAGE".equalsIgnoreCase(provider)) {
			throw new IllegalStateException("Fetched media requires BUNNY_STORAGE for " + mediaType + ", found: " + provider);
		}
	}

	private String getConfig(String traceId, String key) {
		ConfigParameter param = AppCache.configParameters.get(key);
		String value = param != null ? param.getConfigValue() : null;
		if (!StringUtils.hasText(value)) {
			throw new IllegalStateException("Missing Bunny config key: " + key);
		}
		return value;
	}

	private String resolveStorageHost(String traceId) {
		String region = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_REGION);
		String normalized = region.trim().toLowerCase(Locale.ROOT);
		if (normalized.contains(".")) {
			return normalized;
		}
		return normalized + "." + DEFAULT_BUNNY_STORAGE_HOST;
	}

	private String normalizeMediaType(String mediaType) {
		return "VIDEO".equalsIgnoreCase(mediaType) ? "VIDEO" : "IMAGE";
	}

	private static final java.util.Set<String> KNOWN_EXTENSIONS = java.util.Set.of(
			"jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "mp4", "mov", "avi", "webm", "mkv");

	private String resolveExtension(String externalUrl, MediaType contentType, String mediaType) {
		String path = externalUrl;
		int queryIndex = path.indexOf('?');
		if (queryIndex >= 0) {
			path = path.substring(0, queryIndex);
		}
		// Extract last path segment to find extension
		int slashIndex = path.lastIndexOf('/');
		String lastSegment = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
		int dotIndex = lastSegment.lastIndexOf('.');
		if (dotIndex >= 0 && dotIndex < lastSegment.length() - 1) {
			String ext = lastSegment.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
			if (KNOWN_EXTENSIONS.contains(ext)) {
				return ext;
			}
		}
		// Fallback to content-type detection
		if (contentType != null) {
			if (MediaType.IMAGE_JPEG.includes(contentType)) {
				return "jpg";
			}
			if (MediaType.IMAGE_PNG.includes(contentType)) {
				return "png";
			}
			if (contentType.toString().contains("webp")) {
				return "webp";
			}
			if (contentType.toString().contains("mp4")) {
				return "mp4";
			}
		}
		return "VIDEO".equals(mediaType) ? "mp4" : "jpg";
	}

	private MediaType resolveContentType(MediaType contentType, String extension, String mediaType) {
		if (contentType != null) {
			return contentType;
		}
		String guessed = URLConnection.guessContentTypeFromName("file." + extension);
		if (StringUtils.hasText(guessed)) {
			return MediaType.parseMediaType(guessed);
		}
		return "VIDEO".equals(mediaType) ? MediaType.APPLICATION_OCTET_STREAM : MediaType.IMAGE_JPEG;
	}

	private String buildObjectKey(Long organizationId, String mediaType, String extension) {
		LocalDate now = LocalDate.now();
		String folder = "VIDEO".equals(mediaType) ? "videos" : "images";
		return "org/" + organizationId + "/" + folder + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + UUID.randomUUID()
				+ "." + extension;
	}

	private String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	@Getter
	@Builder
	public static class StoredFetchedMedia {
		private final String fileUrl;
		private final String playbackUrl;
		private final String thumbnailUrl;
		private final String storageProvider;
		private final String processingStatus;
		private final String mediaType;
	}
}
