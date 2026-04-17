package com.social.ripple.usermanagement.service.implementation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.usermanagement.dao.model.MediaFile;
import com.social.ripple.usermanagement.dto.response.StoredMediaResult;
import com.social.ripple.usermanagement.service.IMediaStorageService;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.enumeration.MediaType;
import com.social.ripple.usermanagement.util.enumeration.MediaStorageProvider;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagedMediaStorageService {

	private static final String DEFAULT_BUNNY_VIDEO_PLAYBACK_URL = "https://video.bunnycdn.com/play/";
	private static final String DEFAULT_BUNNY_PLAY_DATA_URL = "https://video.bunnycdn.com/library/%s/videos/%s/play";
	private static final String DEFAULT_BUNNY_OEMBED_URL = "https://video.bunnycdn.com/OEmbed";
	private static final String DEFAULT_BUNNY_STORAGE_HOST = "storage.bunnycdn.com";

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final AppCache appCache;

	@Qualifier("localStorageService")
	private final IMediaStorageService localStorageService;

	public StoredMediaResult store(String traceId, MultipartFile file, MediaType mediaType, Long organizationId) throws IOException {
		return switch (mediaType) {
		case IMAGE -> storeImage(traceId, file, organizationId);
		case VIDEO -> storeVideo(traceId, file, organizationId);
		};
	}

	private StoredMediaResult storeImage(String traceId, MultipartFile file, Long organizationId) throws IOException {
		if (!isProviderEnabled(traceId, ConfigKeys.MEDIA_IMAGE_PROVIDER, "BUNNY_STORAGE")) {
			throw new IllegalStateException("Bunny Storage is required for image uploads. Set MEDIA_IMAGE_PROVIDER=BUNNY_STORAGE in config parameters.");
		}

		String zone = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_ZONE, null);
		String accessKey = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_ACCESS_KEY, null);
		String pullZoneBaseUrl = trimTrailingSlash(getConfig(traceId, ConfigKeys.BUNNY_PULL_ZONE_BASE_URL, null));
		if (!StringUtils.hasText(zone) || !StringUtils.hasText(accessKey) || !StringUtils.hasText(pullZoneBaseUrl)) {
			throw new IllegalStateException("Bunny Storage configuration incomplete. Required: BUNNY_STORAGE_ZONE, BUNNY_STORAGE_ACCESS_KEY, BUNNY_PULL_ZONE_BASE_URL");
		}

		String objectKey = buildImageObjectKey(file, organizationId);
		String uploadUrl = "https://" + resolveStorageHost(traceId) + "/" + zone + "/" + objectKey;
		byte[] payload = file.getBytes();

		HttpHeaders headers = new HttpHeaders();
		headers.set("AccessKey", accessKey);
		headers.setContentType(resolveContentType(file, MediaType.IMAGE));
		HttpEntity<byte[]> requestEntity = new HttpEntity<>(payload, headers);

		ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, requestEntity, String.class);
		if (!response.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
			throw new IOException("Bunny image upload failed with status " + response.getStatusCode());
		}

		String fileUrl = pullZoneBaseUrl + "/" + objectKey;
		return StoredMediaResult.builder()
				.fileId(UUID.randomUUID().toString())
				.fileName(objectKey.substring(objectKey.lastIndexOf('/') + 1))
				.fileUrl(fileUrl)
				.providerAssetId(objectKey)
				.storageProvider(MediaStorageProvider.BUNNY_STORAGE)
				.processingStatus("READY")
				.build();
	}

	private StoredMediaResult storeVideo(String traceId, MultipartFile file, Long organizationId) throws IOException {
		if (!isProviderEnabled(traceId, ConfigKeys.MEDIA_VIDEO_PROVIDER, "BUNNY_STREAM")) {
			throw new IllegalStateException("Bunny Stream is required for video uploads. Set MEDIA_VIDEO_PROVIDER=BUNNY_STREAM in config parameters.");
		}

		String libraryId = getConfig(traceId, ConfigKeys.BUNNY_STREAM_LIBRARY_ID, null);
		String apiKey = getConfig(traceId, ConfigKeys.BUNNY_STREAM_API_KEY, null);
		if (!StringUtils.hasText(libraryId) || !StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("Bunny Stream configuration incomplete. Required: BUNNY_STREAM_LIBRARY_ID, BUNNY_STREAM_API_KEY");
		}

		String safeName = buildSafeBaseName(file.getOriginalFilename());
		String title = "org-" + organizationId + "-" + safeName + "-" + System.currentTimeMillis();
		String videoGuid = createBunnyVideo(traceId, libraryId, apiKey, title);
		uploadBunnyVideo(traceId, libraryId, apiKey, videoGuid, file);

		String playbackBaseUrl = trimTrailingSlash(
				getConfig(traceId, ConfigKeys.BUNNY_STREAM_PLAYBACK_BASE_URL, DEFAULT_BUNNY_VIDEO_PLAYBACK_URL));
		String thumbnailBaseUrl = trimTrailingSlash(getConfig(traceId, ConfigKeys.BUNNY_STREAM_THUMBNAIL_BASE_URL, null));
		String playbackUrl = playbackBaseUrl + "/" + libraryId + "/" + videoGuid;

		return StoredMediaResult.builder()
				.fileId(videoGuid)
				.fileName(title)
				.fileUrl(playbackUrl)
				.providerAssetId(videoGuid)
				.playbackUrl(playbackUrl)
				.thumbnailUrl(resolveBunnyThumbnailUrl(traceId, libraryId, apiKey, playbackUrl, thumbnailBaseUrl, videoGuid, "thumbnail.jpg"))
				.storageProvider(MediaStorageProvider.BUNNY_STREAM)
				.processingStatus("PROCESSING")
				.build();
	}

	public Optional<StoredMediaResult> fetchBunnyVideoStatus(String traceId, MediaFile mediaFile) {
		if (mediaFile == null || !MediaStorageProvider.BUNNY_STREAM.name().equalsIgnoreCase(mediaFile.getStorageProvider())) {
			return Optional.empty();
		}

		String libraryId = getConfig(traceId, ConfigKeys.BUNNY_STREAM_LIBRARY_ID, null);
		String apiKey = getConfig(traceId, ConfigKeys.BUNNY_STREAM_API_KEY, null);
		String videoGuid = firstNonBlank(mediaFile.getProviderAssetId(), mediaFile.getFileId());
		if (!StringUtils.hasText(libraryId) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(videoGuid)) {
			log.warn("[{}]|MEDIA|BUNNY_STATUS_SKIPPED|Missing Bunny status config or video guid for mediaId={}", traceId, mediaFile.getId());
			return Optional.empty();
		}

		String playbackBaseUrl = trimTrailingSlash(
				getConfig(traceId, ConfigKeys.BUNNY_STREAM_PLAYBACK_BASE_URL, DEFAULT_BUNNY_VIDEO_PLAYBACK_URL));
		String thumbnailBaseUrl = trimTrailingSlash(getConfig(traceId, ConfigKeys.BUNNY_STREAM_THUMBNAIL_BASE_URL, null));
		String requestUrl = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoGuid;

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("AccessKey", apiKey);
			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
			ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				log.warn("[{}]|MEDIA|BUNNY_STATUS_FAILED|mediaId={} status={}", traceId, mediaFile.getId(), response.getStatusCode());
				return Optional.empty();
			}

			JsonNode jsonNode = objectMapper.readTree(response.getBody());
			Integer statusCode = integerValue(jsonNode, "status");
			String thumbnailFileName = firstNonBlank(textValue(jsonNode, "thumbnailFileName"), "thumbnail.jpg");
			String playbackUrl = StringUtils.hasText(mediaFile.getPlaybackUrl()) ? mediaFile.getPlaybackUrl()
					: playbackBaseUrl + "/" + libraryId + "/" + videoGuid;
			String thumbnailUrl = resolveBunnyThumbnailUrl(traceId, libraryId, apiKey, playbackUrl, thumbnailBaseUrl, videoGuid, thumbnailFileName);

			return Optional.of(StoredMediaResult.builder()
					.fileId(videoGuid)
					.fileName(mediaFile.getFileName())
					.fileUrl(StringUtils.hasText(mediaFile.getFileUrl()) ? mediaFile.getFileUrl() : playbackUrl)
					.providerAssetId(videoGuid)
					.playbackUrl(playbackUrl)
					.thumbnailUrl(thumbnailUrl)
					.storageProvider(MediaStorageProvider.BUNNY_STREAM)
					.processingStatus(mapBunnyProcessingStatus(statusCode))
					.build());
		}
		catch (Exception ex) {
			log.warn("[{}]|MEDIA|BUNNY_STATUS_EXCEPTION|mediaId={} message={}", traceId, mediaFile.getId(), ex.getMessage());
			return Optional.empty();
		}
	}

	public Optional<StoredMediaResult> fetchLocalVideoMetadata(String traceId, MediaFile mediaFile) {
		if (mediaFile == null || !MediaStorageProvider.LOCAL.name().equalsIgnoreCase(mediaFile.getStorageProvider())
				|| !"video".equalsIgnoreCase(mediaFile.getFileType())) {
			return Optional.empty();
		}

		String playbackUrl = StringUtils.hasText(mediaFile.getPlaybackUrl()) ? mediaFile.getPlaybackUrl() : mediaFile.getFileUrl();
		String thumbnailUrl = StringUtils.hasText(mediaFile.getThumbnailUrl()) ? mediaFile.getThumbnailUrl()
				: generateLocalVideoThumbnail(traceId, mediaFile.getProviderAssetId(), mediaFile.getFileId());

		return Optional.of(StoredMediaResult.builder()
				.fileId(mediaFile.getFileId())
				.fileName(mediaFile.getFileName())
				.fileUrl(mediaFile.getFileUrl())
				.providerAssetId(mediaFile.getProviderAssetId())
				.playbackUrl(playbackUrl)
				.thumbnailUrl(thumbnailUrl)
				.storageProvider(MediaStorageProvider.LOCAL)
				.processingStatus(firstNonBlank(mediaFile.getProcessingStatus(), "READY"))
				.build());
	}

	private String resolveBunnyThumbnailUrl(String traceId, String libraryId, String apiKey, String playbackUrl, String thumbnailBaseUrl, String videoGuid,
			String thumbnailFileName) {
		if (StringUtils.hasText(thumbnailBaseUrl)) {
			return thumbnailBaseUrl + "/" + videoGuid + "/" + thumbnailFileName;
		}
		String playDataThumbnailUrl = fetchBunnyPlayDataThumbnailUrl(traceId, libraryId, apiKey, videoGuid);
		if (StringUtils.hasText(playDataThumbnailUrl)) {
			return playDataThumbnailUrl;
		}
		if (!StringUtils.hasText(playbackUrl)) {
			return null;
		}

		try {
			String requestUrl = UriComponentsBuilder.fromHttpUrl(DEFAULT_BUNNY_OEMBED_URL)
					.queryParam("url", playbackUrl)
					.build()
					.toUriString();
			ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
				return null;
			}

			JsonNode jsonNode = objectMapper.readTree(response.getBody());
			return firstNonBlank(textValue(jsonNode, "thumbnail_url"), textValue(jsonNode, "thumbnailUrl"));
		}
		catch (Exception ex) {
			log.debug("[{}]|MEDIA|BUNNY_OEMBED_THUMBNAIL_FAILED|videoGuid={} message={}", traceId, videoGuid, ex.getMessage());
			return null;
		}
	}

	private String fetchBunnyPlayDataThumbnailUrl(String traceId, String libraryId, String apiKey, String videoGuid) {
		if (!StringUtils.hasText(libraryId) || !StringUtils.hasText(videoGuid)) {
			return null;
		}

		try {
			HttpHeaders headers = new HttpHeaders();
			if (StringUtils.hasText(apiKey)) {
				headers.set("AccessKey", apiKey);
			}
			HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
			String requestUrl = String.format(DEFAULT_BUNNY_PLAY_DATA_URL, libraryId, videoGuid);
			ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
				return null;
			}

			JsonNode jsonNode = objectMapper.readTree(response.getBody());
			return firstNonBlank(findFirstTextValue(jsonNode, "thumbnailUrl"), findFirstTextValue(jsonNode, "thumbnail_url"));
		}
		catch (Exception ex) {
			log.debug("[{}]|MEDIA|BUNNY_PLAYDATA_THUMBNAIL_FAILED|videoGuid={} message={}", traceId, videoGuid, ex.getMessage());
			return null;
		}
	}

	private StoredMediaResult storeLocally(String traceId, MultipartFile file, MediaType mediaType) throws IOException {
		String uuid = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
		String storedFileName = localStorageService.storeFile(traceId, file, uuid);
		String baseUrl = getConfig(traceId, ConfigKeys.MEDIA_FILE_STORAGE_BASE_URL, ApplicationConstants.MEDIA_FILE_STORAGE_BASE_URL);
		String thumbnailUrl = mediaType == MediaType.VIDEO ? generateLocalVideoThumbnail(traceId, storedFileName, uuid) : null;
		return StoredMediaResult.builder()
				.fileId(uuid)
				.fileName(storedFileName)
				.fileUrl(baseUrl + storedFileName)
				.providerAssetId(storedFileName)
				.playbackUrl(mediaType == MediaType.VIDEO ? baseUrl + storedFileName : null)
				.thumbnailUrl(thumbnailUrl)
				.storageProvider(MediaStorageProvider.LOCAL)
				.processingStatus("READY")
				.build();
	}

	private String generateLocalVideoThumbnail(String traceId, String storedFileName, String fileId) {
		if (!StringUtils.hasText(storedFileName)) {
			return null;
		}

		String storagePath = getConfig(traceId, ConfigKeys.MEDIA_FILE_STORAGE_PATH, ApplicationConstants.MEDIA_FILE_STORAGE_PATH);
		String baseUrl = getConfig(traceId, ConfigKeys.MEDIA_FILE_STORAGE_BASE_URL, ApplicationConstants.MEDIA_FILE_STORAGE_BASE_URL);
		File videoFile = Path.of(storagePath, storedFileName).toFile();
		if (!videoFile.exists()) {
			log.warn("[{}]|MEDIA|LOCAL_THUMBNAIL_SKIPPED|Video file missing for thumbnail generation path={}", traceId, videoFile.getAbsolutePath());
			return null;
		}

		String thumbnailFileName = stripExtension(storedFileName) + "-thumb.jpg";
		File thumbnailFile = Path.of(storagePath, thumbnailFileName).toFile();
		if (thumbnailFile.exists()) {
			return baseUrl + thumbnailFileName;
		}

		try (SeekableByteChannel channel = NIOUtils.readableChannel(videoFile)) {
			FrameGrab frameGrab = FrameGrab.createFrameGrab(channel);
			Picture picture = frameGrab.getNativeFrame();
			if (picture == null) {
				return null;
			}

			BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
			ImageIO.write(bufferedImage, "jpg", thumbnailFile);
			log.info("[{}]|MEDIA|LOCAL_THUMBNAIL_CREATED|fileId={} thumbnail={}", traceId, fileId, thumbnailFileName);
			return baseUrl + thumbnailFileName;
		}
		catch (IOException | JCodecException ex) {
			log.warn("[{}]|MEDIA|LOCAL_THUMBNAIL_FAILED|fileId={} message={}", traceId, fileId, ex.getMessage());
			return null;
		}
	}

	private String createBunnyVideo(String traceId, String libraryId, String apiKey, String title) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("AccessKey", apiKey);
		headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>("{\"title\":\"" + escapeJson(title) + "\"}", headers);
		String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos";
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IOException("Bunny video create failed with status " + response.getStatusCode());
		}

		JsonNode jsonNode = objectMapper.readTree(response.getBody());
		String guid = firstNonBlank(
				textValue(jsonNode, "guid"),
				textValue(jsonNode, "Guid"),
				textValue(jsonNode, "videoGuid"),
				textValue(jsonNode, "VideoGuid"));
		if (!StringUtils.hasText(guid)) {
			throw new IOException("Bunny video create succeeded but response did not include a guid");
		}
		log.info("[{}]|MEDIA|BUNNY_VIDEO_CREATED|Guid={}", traceId, guid);
		return guid;
	}

	private void uploadBunnyVideo(String traceId, String libraryId, String apiKey, String videoGuid, MultipartFile file) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("AccessKey", apiKey);
		headers.setContentType(resolveContentType(file, MediaType.VIDEO));
		ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
			@Override
			public String getFilename() {
				return file.getOriginalFilename();
			}
		};
		HttpEntity<ByteArrayResource> requestEntity = new HttpEntity<>(resource, headers);
		String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoGuid;
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
		if (!response.getStatusCode().is2xxSuccessful() && !response.getStatusCode().is3xxRedirection()) {
			throw new IOException("Bunny video upload failed with status " + response.getStatusCode());
		}
		log.info("[{}]|MEDIA|BUNNY_VIDEO_UPLOADED|Guid={}", traceId, videoGuid);
	}

	private boolean isProviderEnabled(String traceId, String configKey, String expectedValue) {
		String provider = getConfig(traceId, configKey, null);
		return expectedValue.equalsIgnoreCase(provider);
	}

	private String resolveStorageHost(String traceId) {
		String region = getConfig(traceId, ConfigKeys.BUNNY_STORAGE_REGION, null);
		if (!StringUtils.hasText(region)) {
			return DEFAULT_BUNNY_STORAGE_HOST;
		}
		String normalized = region.trim().toLowerCase(Locale.ROOT);
		if (normalized.contains(".")) {
			return normalized;
		}
		return normalized + "." + DEFAULT_BUNNY_STORAGE_HOST;
	}

	private String buildImageObjectKey(MultipartFile file, Long organizationId) {
		LocalDate now = LocalDate.now();
		String extension = getExtension(file.getOriginalFilename());
		String fileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
		return "org/" + organizationId + "/images/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue()) + "/" + fileName;
	}

	private String getExtension(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return "";
		}
		int idx = originalFilename.lastIndexOf('.');
		if (idx < 0 || idx == originalFilename.length() - 1) {
			return "";
		}
		return originalFilename.substring(idx + 1).toLowerCase(Locale.ROOT);
	}

	private String buildSafeBaseName(String originalFilename) {
		String source = StringUtils.hasText(originalFilename) ? originalFilename : "video";
		String base = source;
		int idx = source.lastIndexOf('.');
		if (idx > 0) {
			base = source.substring(0, idx);
		}
		return base.replaceAll("[^A-Za-z0-9_-]", "-");
	}

	private org.springframework.http.MediaType resolveContentType(MultipartFile file, MediaType mediaType) {
		if (StringUtils.hasText(file.getContentType())) {
			return org.springframework.http.MediaType.parseMediaType(file.getContentType());
		}
		return mediaType == MediaType.IMAGE ? org.springframework.http.MediaType.IMAGE_JPEG
				: org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
	}

	private String getConfig(String traceId, String key, String fallback) {
		String value = appCache.getConfigParameterValue(traceId, key);
		return StringUtils.hasText(value) ? value : fallback;
	}

	private String trimTrailingSlash(String input) {
		if (!StringUtils.hasText(input)) {
			return input;
		}
		return input.endsWith("/") ? input.substring(0, input.length() - 1) : input;
	}

	private String textValue(JsonNode node, String field) {
		JsonNode child = node.get(field);
		return child == null || child.isNull() ? null : child.asText();
	}

	private String findFirstTextValue(JsonNode node, String field) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isObject()) {
			JsonNode directChild = node.get(field);
			if (directChild != null && !directChild.isNull() && directChild.isTextual()) {
				return directChild.asText();
			}
			Iterator<JsonNode> childIterator = node.elements();
			while (childIterator.hasNext()) {
				String value = findFirstTextValue(childIterator.next(), field);
				if (StringUtils.hasText(value)) {
					return value;
				}
			}
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				String value = findFirstTextValue(child, field);
				if (StringUtils.hasText(value)) {
					return value;
				}
			}
		}
		return null;
	}

	private Integer integerValue(JsonNode node, String field) {
		JsonNode child = node.get(field);
		return child == null || child.isNull() ? null : child.asInt();
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
	}

	private String escapeJson(String value) {
		return value == null ? "" : new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}

	private String stripExtension(String fileName) {
		if (!StringUtils.hasText(fileName)) {
			return fileName;
		}
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex <= 0) {
			return fileName;
		}
		return fileName.substring(0, dotIndex);
	}

	private String mapBunnyProcessingStatus(Integer statusCode) {
		if (statusCode == null) {
			return "PROCESSING";
		}
		return switch (statusCode) {
		case 3, 4 -> "READY";
		case 5, 8 -> "FAILED";
		default -> "PROCESSING";
		};
	}
}
