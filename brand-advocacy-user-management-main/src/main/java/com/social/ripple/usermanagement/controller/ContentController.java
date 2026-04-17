/**
 * Filename: ContentController.java
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

import com.social.ripple.usermanagement.dto.request.TrendingTopicsRequestDTO;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ContentGenerateResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.social.ripple.usermanagement.dto.request.ContentGenerateRequestDTO;
import com.social.ripple.usermanagement.dto.response.ContentGenerateResponseDTO;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.service.IContentService;
import com.social.ripple.usermanagement.util.ResponseUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/v1/content")
public class ContentController {

    private final IContentService contentService;

    @Autowired
    public ContentController(IContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateContent(
            @RequestHeader("x-trace-id") String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader("x-tenant-id") String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody ContentGenerateRequestDTO requestDTO) {

        log.info("[{}]|CONTENT_CONTROLLER|Received generateContent request", traceId);

        if (traceId == null || traceId.isBlank() || tenantId == null || tenantId.isBlank()) {
            log.error("[{}]|CONTENT_CONTROLLER|Missing headers", traceId);
            return ResponseEntity.badRequest().body(new ErrorObj("400", "Missing required headers","",""));
        }
        if (requestDTO == null || requestDTO.getTopic() == null || requestDTO.getTopic().isBlank()) {
            log.error("[{}]|CONTENT_CONTROLLER|Invalid request payload", traceId);
            return ResponseEntity.badRequest().body(new ErrorObj("400", "Topic must not be null or empty","",""));
        }

        ContentGenerateResponse response = contentService.generateContent(traceId, correlationId, tenantId, userDetails, requestDTO);
        return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
    }

    @GetMapping("/trending-hashtags")
    public ResponseEntity<?> getTrendingHashtags(
            @RequestHeader(value="x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false ) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId
    ) {

        log.info("[{}]|CONTENT_CONTROLLER|Received generateContent request", traceId);

        ContentGenerateResponse response = contentService.getTrendingHashtags(traceId, correlationId, tenantId);
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending-topics")
    public ResponseEntity<?> getTrendingTopics(
            @RequestHeader(value="x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false ) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId
    ) {

        log.info("[{}]|CONTENT_CONTROLLER|Received generateContent request", traceId);

        ContentGenerateResponse response = contentService.getTrendingTopics(traceId, correlationId, tenantId);
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trending-topics/preferences")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getTrendingTopicPreferences(
            @RequestHeader(value="x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false ) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId
    ) {

        log.info("[{}]|CONTENT_CONTROLLER|Received trending topic preferences request", traceId);

        ContentGenerateResponse response = contentService.getTrendingTopicPreferences(traceId, correlationId, tenantId);
        response.setStatus(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trending-topics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> createTrendingTopics(
            @RequestHeader(value = "x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) TrendingTopicsRequestDTO requestDTO
    ) {
        ContentGenerateResponse response = contentService.createCustomTrendingTopics(
                traceId, correlationId, tenantId, userDetails, requestDTO
        );
        return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
    }

    @PutMapping("/trending-topics")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateTrendingTopics(
            @RequestHeader(value = "x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) TrendingTopicsRequestDTO requestDTO
    ) {
        ContentGenerateResponse response = contentService.updateCustomTrendingTopics(
                traceId, correlationId, tenantId, userDetails, requestDTO
        );
        return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
    }

    @GetMapping("fetch/trending-hashtags")
    public ResponseEntity<?> fetchTrendingHashtags(
            @RequestHeader(value="x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false ) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId
            ) {

        log.info("[{}]|CONTENT_CONTROLLER|Received generateContent request", traceId);

        contentService.fetchTrendingHashtags(traceId);

        return ResponseEntity.ok(buildResponse(true, "200", "Fetching", null, null));
    }

    @GetMapping("fetch/trending-topics")
    public ResponseEntity<?> fetchTrendingTopics(
            @RequestHeader(value="x-trace-id", required = false) String traceId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "x-tenant-id", required = false ) String tenantId,
            @RequestHeader(value = "language-id", required = false) String languageId
    ) {

        log.info("[{}]|CONTENT_CONTROLLER|Received generateContent request", traceId);

        contentService.fetchTrendingTopics(traceId);

        return ResponseEntity.ok(buildResponse(true, "200", "Fetching", null, null));
    }

    private BaseResponse buildResponse(boolean status, String code, String message, String devMessage, ErrorObj error) {
        BaseResponse response = new BaseResponse();
        response.setStatus(status);
        response.setCode(code);
        response.setMessage(message);
        response.setDevMessage(devMessage);
        response.setTimestamp(new Date());
        response.setErrors(error != null ? Collections.singletonList(error) : null);
        return response;
    }
}
