package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.external_ingestion.constants.ApplicationConstants;
import com.social.ripple.external_ingestion.dao.repository.NotificationRepository;
import com.social.ripple.external_ingestion.dto.request.UploadPart;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.ConfigKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LinkedinHelper {

	@Autowired
	private AppCache appCache;
	@Autowired
	private NotificationRepository notificationRepository;

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private RestTemplate restTemplate;

	private static final int CHUNK_SIZE = 4 * 1024 * 1024;


	public JsonNode createUploadFinalizeRequest(List<UploadPart> uploadIds, String videoUrn) {

		ObjectNode finalizeUploadRequest = mapper.createObjectNode();

		// Create the inner request data
		ObjectNode requestData = mapper.createObjectNode();
		requestData.put("video", videoUrn);
		requestData.put("uploadToken", "");

		// Create array for uploaded part IDs
		ArrayNode uploadedPartIdsArray = mapper.createArrayNode();
		uploadIds.stream()
				.map(UploadPart::getEtag)
				.filter(Objects::nonNull) // Filter out null values
				.forEach(uploadedPartIdsArray::add);

		requestData.set("uploadedPartIds", uploadedPartIdsArray);

		// Wrap in the outer structure
		finalizeUploadRequest.set("finalizeUploadRequest", requestData);

		return finalizeUploadRequest;
	}


	public List<UploadPart> uploadVideoInChunks(String uploadUrl, byte[] videoData, String fileName, String token) {
		List<UploadPart> uploadedParts = new ArrayList<>();
		long fileSize = videoData.length;

		log.info("Uploading video data: {} bytes in {} byte chunks. File: {}", fileSize, CHUNK_SIZE, fileName);

		try {
			long totalBytesProcessed = 0;
			int partNumber = 1;

			while (totalBytesProcessed < fileSize) {
				int chunkLength = (int) Math.min(CHUNK_SIZE, fileSize - totalBytesProcessed);
				long startByte = totalBytesProcessed;
				long endByte = totalBytesProcessed + chunkLength - 1;

				// Extract chunk from byte array
				byte[] chunkData = Arrays.copyOfRange(videoData, (int) startByte, (int) (startByte + chunkLength));

				log.debug("Uploading chunk {}: bytes {}-{} (size: {} bytes)",
						partNumber, startByte, endByte, chunkData.length);

				UploadPart part = uploadChunk(uploadUrl, chunkData, startByte, endByte, fileSize, partNumber, token);
				uploadedParts.add(part);

				totalBytesProcessed += chunkLength;
				partNumber++;

				// Progress logging
				if (partNumber % 5 == 0 || totalBytesProcessed == fileSize) {
					double progress = (double) totalBytesProcessed / fileSize * 100;
					log.info("Upload progress: {}/{} bytes ({}%)",
							totalBytesProcessed, fileSize, String.format("%.2f", progress));
				}
			}

			log.info("Upload completed: {} chunks uploaded successfully for file: {}",
					uploadedParts.size(), fileName);

		} catch (Exception e) {
			throw new RuntimeException("Failed to upload video chunks from byte array for file: " + fileName, e);
		}

		return uploadedParts;
	}

	private UploadPart uploadChunk(String uploadUrl, byte[] chunkData, long startByte, long endByte,
								   long totalSize, int partNumber, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		headers.set("X-Restli-Protocol-Version", "2.0.0");
		headers.set("LinkedIn-Version", "202509");
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.set("Content-Range", String.format("bytes %d-%d/%d", startByte, endByte, totalSize));



		log.info("before upload chunk length: {}",chunkData.length);

		HttpEntity<byte[]> entity = new HttpEntity<>(chunkData, headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
					uploadUrl, HttpMethod.PUT, entity, String.class);

			log.info("after upload chunk : {}",response);

			if (response.getStatusCode().is2xxSuccessful()) {
				// Extract ETag from response headers
				String etag = response.getHeaders().getFirst("ETag");
				if (etag == null || etag.trim().isEmpty()) {
					// If no ETag provided, generate one based on part number
					etag = "part-" + partNumber + "-" + startByte + "-" + endByte;
					log.warn("No ETag received from server for chunk {}. Using generated ETag: {}", partNumber, etag);
				} else {
					// Remove quotes from ETag if present
					etag = etag.replace("\"", "");
					log.debug("Received ETag for chunk {}: {}", partNumber, etag);
				}

				return new UploadPart(etag, startByte, endByte);

			} else {
				throw new RuntimeException("Chunk upload failed for bytes " + startByte + "-" + endByte +
						". Status: " + response.getStatusCode() +
						", Response: " + response.getBody());
			}
		} catch (Exception e) {
			log.error("chunk upload error: {}",e);
			throw new RuntimeException("Failed to upload chunk " + partNumber + " (bytes " + startByte +
					"-" + endByte + ")", e);
		}
	}

	public String storeFile(String traceId, MultipartFile file, String uuid) throws IOException {
		String originalFileName = file.getOriginalFilename();
		String fileExtension = "";
		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
			fileExtension = originalFileName.substring(dotIndex + 1);
		}

		String storedFileName = uuid + "." + fileExtension;
		String fileStorrePath = appCache.getConfigParameterValue(traceId, ConfigKeys.MEDIA_FILE_STORAGE_PATH);
		if (fileStorrePath == null) {
			fileStorrePath = ApplicationConstants.MEDIA_FILE_STORAGE_PATH;
			log.info("[{}]|MEDIA|SETTING_DEFAULT|Seting default file storage path : {}", traceId, fileStorrePath);
		}
		File destFile = new File(fileStorrePath + storedFileName);
		file.transferTo(destFile);
		log.info("[{}]|MEDIA|LOCAL_FILE_SAVED|Path: {}", traceId, destFile.getAbsolutePath());

		return storedFileName;

	}
}
