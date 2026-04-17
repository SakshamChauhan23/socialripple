/**
 * Filename: ArchiveServiceImpl.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.MediaLibrary;
import com.social.ripple.usermanagement.dao.repository.MediaLibraryRepository;
import com.social.ripple.usermanagement.dto.response.ArchiveResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IArchiveService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ArchiveServiceImpl implements IArchiveService {

    private final MediaLibraryRepository mediaLibraryRepository;

    @Autowired
    public ArchiveServiceImpl(MediaLibraryRepository mediaLibraryRepository) {
        this.mediaLibraryRepository = mediaLibraryRepository;
    }

    @Override
    public ArchiveResponse archiveMedia(String xTraceId, String xCorrelationId, String tenantId, 
                                       String languageId, UserDetailsImpl detailsImpl, Long mediaId) {
        log.info("[{}]|ARCHIVE_SERVICE|archiveMedia|Starting media archive process for mediaId: {}", 
                xTraceId, mediaId);
        log.debug("[{}]|ARCHIVE_SERVICE|archiveMedia|Input params - tenantId: {}, languageId: {}, mediaId: {}", 
                 xTraceId, tenantId, languageId, mediaId);

        ArchiveResponse response = new ArchiveResponse();

        try {
            // Validate input parameters
            if (mediaId == null || mediaId <= 0) {
                log.error("[{}]|ARCHIVE_SERVICE|archiveMedia|Invalid media ID: {}", xTraceId, mediaId);
                return buildErrorResponse(ResponseCode.USMG_400, "Invalid media ID provided.", false);
            }

            if (detailsImpl == null || detailsImpl.getOrganization() == null) {
                log.error("[{}]|ARCHIVE_SERVICE|archiveMedia|Invalid user details or organization", xTraceId);
                return buildErrorResponse(ResponseCode.USMG_403, "Unauthorized access.", false);
            }

            // Get user's organization ID
            Long userOrgId = detailsImpl.getOrganization().getId();
            log.debug("[{}]|ARCHIVE_SERVICE|archiveMedia|User organization ID: {}", xTraceId, userOrgId);

            // Validate tenant ID matches user's organization (if provided)
            if (tenantId != null && !tenantId.equals(String.valueOf(userOrgId))) {
                log.error("[{}]|ARCHIVE_SERVICE|archiveMedia|Tenant ID mismatch - provided: {}, user org: {}", 
                         xTraceId, tenantId, userOrgId);
                return buildErrorResponse(ResponseCode.USMG_403, "Access denied for this tenant.", false);
            }

            // Find media by ID and organization
            log.debug("[{}]|ARCHIVE_SERVICE|archiveMedia|Searching for media with ID: {} in organization: {}", 
                     xTraceId, mediaId, userOrgId);
            
            MediaLibrary media = mediaLibraryRepository.findByIdAndOrganizationId(mediaId, userOrgId)
                    .orElse(null);
                    
            if (media == null) {
                log.error("[{}]|ARCHIVE_SERVICE|archiveMedia|Media not found - ID: {}, orgId: {}", 
                         xTraceId, mediaId, userOrgId);
                return buildErrorResponse(ResponseCode.USMG_404, "Media not found.", false);
            }

            log.info("[{}]|ARCHIVE_SERVICE|archiveMedia|Media found - ID: {}, title: {}, current archived status: {}", 
                    xTraceId, media.getId(), media.getTitle(), media.getArchived());

            // Check if media is already archived
            if (Boolean.TRUE.equals(media.getArchived())) {
                log.warn("[{}]|ARCHIVE_SERVICE|archiveMedia|Media already archived - ID: {}", xTraceId, mediaId);
                return buildErrorResponse(ResponseCode.USMG_409, "Media is already archived.", false);
            }

            // Archive the media
            media.setArchived(true);
            media.setUpdatedAt(LocalDateTime.now());
            media.setUpdatedBy(detailsImpl.getUserId());

            log.debug("[{}]|ARCHIVE_SERVICE|archiveMedia|Updating media - ID: {}, archived: true, updatedBy: {}", 
                     xTraceId, mediaId, detailsImpl.getUserId());

            mediaLibraryRepository.save(media);
            
            log.info("[{}]|ARCHIVE_SERVICE|archiveMedia|Media archived successfully - ID: {}", xTraceId, mediaId);

            // Build success response
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Media archived successfully.");
            response.setTimestamp(new Date());

            return response;

        } catch (Exception e) {
            log.error("[{}]|ARCHIVE_SERVICE|archiveMedia|Unexpected error occurred - {}", 
                     xTraceId, e.getMessage(), e);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error occurred.", false);
        }
    }

    @Override
    public ArchiveResponse restoreMedia(String xTraceId, String xCorrelationId, String tenantId, 
                                       String languageId, UserDetailsImpl detailsImpl, Long mediaId) {
        log.info("[{}]|ARCHIVE_SERVICE|restoreMedia|Starting media restore process for mediaId: {}", 
                xTraceId, mediaId);
        log.debug("[{}]|ARCHIVE_SERVICE|restoreMedia|Input params - tenantId: {}, languageId: {}, mediaId: {}", 
                 xTraceId, tenantId, languageId, mediaId);

        ArchiveResponse response = new ArchiveResponse();

        try {
            // Validate input parameters
            if (mediaId == null || mediaId <= 0) {
                log.error("[{}]|ARCHIVE_SERVICE|restoreMedia|Invalid media ID: {}", xTraceId, mediaId);
                return buildErrorResponse(ResponseCode.USMG_400, "Invalid media ID provided.", false);
            }

            if (detailsImpl == null || detailsImpl.getOrganization() == null) {
                log.error("[{}]|ARCHIVE_SERVICE|restoreMedia|Invalid user details or organization", xTraceId);
                return buildErrorResponse(ResponseCode.USMG_401, "Unauthorized access.", false);
            }

            // Get user's organization ID
            Long userOrgId = detailsImpl.getOrganization().getId();
            log.debug("[{}]|ARCHIVE_SERVICE|restoreMedia|User organization ID: {}", xTraceId, userOrgId);

            // Validate tenant ID matches user's organization (if provided)
            if (tenantId != null && !tenantId.equals(String.valueOf(userOrgId))) {
                log.error("[{}]|ARCHIVE_SERVICE|restoreMedia|Tenant ID mismatch - provided: {}, user org: {}", 
                         xTraceId, tenantId, userOrgId);
                return buildErrorResponse(ResponseCode.USMG_403, "Access denied for this tenant.", false);
            }

            // Find media by ID and organization
            log.debug("[{}]|ARCHIVE_SERVICE|restoreMedia|Searching for media with ID: {} in organization: {}", 
                     xTraceId, mediaId, userOrgId);
            
            MediaLibrary media = mediaLibraryRepository.findByIdAndOrganizationId(mediaId, userOrgId)
                    .orElse(null);
                    
            if (media == null) {
                log.error("[{}]|ARCHIVE_SERVICE|restoreMedia|Media not found - ID: {}, orgId: {}", 
                         xTraceId, mediaId, userOrgId);
                return buildErrorResponse(ResponseCode.USMG_404, "Media not found.", false);
            }

            log.info("[{}]|ARCHIVE_SERVICE|restoreMedia|Media found - ID: {}, title: {}, current archived status: {}", 
                    xTraceId, media.getId(), media.getTitle(), media.getArchived());

            // Check if media is not archived
            if (Boolean.FALSE.equals(media.getArchived())) {
                log.warn("[{}]|ARCHIVE_SERVICE|restoreMedia|Media is not archived - ID: {}", xTraceId, mediaId);
                return buildErrorResponse(ResponseCode.USMG_409, "Media is not archived.", false);
            }

            // Restore the media
            media.setArchived(false);
            media.setUpdatedAt(LocalDateTime.now());
            media.setUpdatedBy(detailsImpl.getUserId());

            log.debug("[{}]|ARCHIVE_SERVICE|restoreMedia|Updating media - ID: {}, archived: false, updatedBy: {}", 
                     xTraceId, mediaId, detailsImpl.getUserId());

            mediaLibraryRepository.save(media);
            
            log.info("[{}]|ARCHIVE_SERVICE|restoreMedia|Media restored successfully - ID: {}", xTraceId, mediaId);

            // Build success response
            response.setStatus(true);
            response.setCode("MEDIA_200");
            response.setMessage("Media restored successfully.");
            response.setTimestamp(new Date());

            return response;

        } catch (Exception e) {
            log.error("[{}]|ARCHIVE_SERVICE|restoreMedia|Unexpected error occurred - {}", 
                     xTraceId, e.getMessage(), e);
            return buildErrorResponse(ResponseCode.USMG_500, "Internal server error occurred.", false);
        }
    }

   
    private ArchiveResponse buildErrorResponse(String code, String message, boolean status) {
        ArchiveResponse response = new ArchiveResponse();
        response.setStatus(status);
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(new Date());
        return response;
     }
}