package com.social.ripple.usermanagement.controller;

import java.net.URI;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dao.model.MediaFile;
import com.social.ripple.usermanagement.dao.repository.MediaFileRepository;
import com.social.ripple.usermanagement.dto.response.MediaContentResponse;
import com.social.ripple.usermanagement.service.implementation.MediaContentServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/media/content")
@RequiredArgsConstructor
@Slf4j
public class MediaContentController {

    private final MediaContentServiceImpl mediaService;
    private final MediaFileRepository mediaFileRepository;

    @GetMapping
    public ResponseEntity<?> getMediaById(@RequestParam("file_id") String fileId) {

        try {
            // Check if media is stored on CDN — redirect instead of serving from disk
            Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByFileId(fileId);
            if (mediaFileOpt.isPresent()) {
                MediaFile mediaFile = mediaFileOpt.get();
                String provider = mediaFile.getStorageProvider();
                String fileUrl = mediaFile.getFileUrl();
                if (("BUNNY_STORAGE".equals(provider) || "BUNNY_STREAM".equals(provider)) && fileUrl != null) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(fileUrl))
                            .header("Cache-Control", "public, max-age=86400")
                            .build();
                }
            }

            // Fallback: serve from local disk (legacy files)
            MediaContentResponse mediaResponse = mediaService.getMediaFile(fileId);

            if (mediaResponse == null || mediaResponse.getResource() == null) {
                log.warn("File not found for file_id={}", fileId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Media file not found: " + fileId);
            }

            Resource resource = mediaResponse.getResource();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mediaResponse.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + mediaResponse.getTitle() + "\"")
                    .body(resource);

        } catch (Exception ex) {
            log.error("Error fetching file_id={} -> {}", fileId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error: " + ex.getMessage());
        }
    }
}
