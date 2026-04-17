package com.social.ripple.usermanagement.service.implementation;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dto.response.MediaContentResponse;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaContentServiceImpl {

    private final AppCache appCache;

    public MediaContentResponse getMediaFile(String fileId) {
        log.info("Fetching media file by fileId={}", fileId);

        String baseFolder = appCache.getConfigParameterValue(null, ConfigKeys.MEDIA_FILE_STORAGE_PATH);
        if (baseFolder == null || baseFolder.trim().isEmpty()) {
            log.error("Server folder path not configured!");
            return null;
        }

        File file = new File(baseFolder, fileId);

        if (!file.exists() || !file.isFile()) {
            log.warn("File does not exist for fileId={}, expected path={}", fileId, file.getAbsolutePath());
            return null;
        }

        MediaContentResponse response = new MediaContentResponse();
        response.setTitle(fileId);
        response.setFileType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setUrl(file.getAbsolutePath());
        Resource resource = new FileSystemResource(file);
        response.setResource(resource);

        log.info("Media file loaded successfully: {}", file.getAbsolutePath());
        return response;
    }
}
