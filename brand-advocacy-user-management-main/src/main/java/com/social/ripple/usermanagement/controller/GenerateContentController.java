/**
 * Filename: GenerateContentController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
 * property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this
 * software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements
 * explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.GenerateContentRequestDTO;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.GenerateContentResponse;
import com.social.ripple.usermanagement.service.IGenerateContentService;
import com.social.ripple.usermanagement.util.ResponseUtils;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/content")
public class GenerateContentController {

    private final IGenerateContentService generateContentService;

    @Autowired
    public GenerateContentController(IGenerateContentService generateContentService) {
        this.generateContentService = generateContentService;
    }

    @PostMapping("/summarize")
    public ResponseEntity<?> summarizePost(@RequestHeader("x-trace-id")
                                           String traceId, @RequestHeader(value = "x-correlation-id", required = false)
                                           String correlationId, @RequestHeader("x-tenant-id")
                                           String tenantId, @RequestHeader(value = "language-id", required = false)
                                           String languageId, @RequestBody
                                           GenerateContentRequestDTO requestDTO) {
        log.info("[{}]|GENERATE_CONTENT|Controller|Start summarize API", traceId);

        // ✅ Header validation
        if (traceId == null || traceId.isBlank() || tenantId == null || tenantId.isBlank()) {
            GenerateContentResponse resp = new GenerateContentResponse();
            resp.setStatus(false);
            resp.setCode(ResponseCode.USMG_400);
            resp.setMessage("Missing required headers");
            resp.setDevMessage("x-trace-id and x-tenant-id must be provided");
            resp.setTimestamp(new Date());
            resp.setErrors(List.of(new ErrorObj("/v1/content/summarize", ResponseCode.USMG_400, "Header Validation Error",
                    "x-trace-id and x-tenant-id are required")));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }


        GenerateContentResponse response = generateContentService.generateSummary(traceId, tenantId, requestDTO);

        return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
    }
}
