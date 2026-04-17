/**
 * Filename: ArchiveController.java
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
package com.social.ripple.usermanagement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.response.ArchiveResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IArchiveService;
import com.social.ripple.usermanagement.util.ResponseUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController 
public class ArchiveController {
	 private final IArchiveService archiveService;
	    @Autowired
	    public ArchiveController(IArchiveService archiveService) {
	        this.archiveService=archiveService;
	    }

	    @PostMapping("{version}/media/{mediaId}/archive")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<ArchiveResponse> archiveMediaLibrary(
	            @PathVariable("version") String version,
	            @PathVariable("mediaId") Long mediaId,
	            @RequestHeader(value = "x-trace-id", required = true) String traceId,
	            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
	            @RequestHeader(value = "x-tenant-id", required = true) String tenantId,
	            @RequestHeader(value = "language-id", required = false) String languageId,
	            @AuthenticationPrincipal UserDetailsImpl detailsImpl) {

	    	log.info("[{}]|ARCHIVE|Controller|Create Archive request received", traceId);
	    	ArchiveResponse response= archiveService.archiveMedia(traceId, correlationId, tenantId, languageId, detailsImpl,mediaId);
	    	 return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	        
	    }
	    
	    @PostMapping("{version}/media/{mediaId}/restore")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<ArchiveResponse> restoreMediaLibrary(
	            @PathVariable("version") String version,
	            @PathVariable("mediaId") Long mediaId,
	            @RequestHeader(value = "x-trace-id", required = true) String traceId,
	            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
	            @RequestHeader(value = "x-tenant-id", required = true) String tenantId,
	            @RequestHeader(value = "language-id", required = false) String languageId,
	            @AuthenticationPrincipal UserDetailsImpl detailsImpl) { 

	    	log.info("[{}]|ARCHIVE|Controller|Create Archive request received", traceId);
	    	ArchiveResponse response= archiveService.restoreMedia(traceId, correlationId, tenantId, languageId, detailsImpl,mediaId);
	    	 return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	        
	    }

}

