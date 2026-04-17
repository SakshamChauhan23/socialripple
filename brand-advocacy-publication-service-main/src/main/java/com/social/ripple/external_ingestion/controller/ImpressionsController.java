package com.social.ripple.external_ingestion.controller;

import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.BaseResponse;
import com.social.ripple.external_ingestion.dto.response.ErrorObj;
import com.social.ripple.external_ingestion.dto.response.ImpressionsResponse;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.XConnectionService;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/impressions")
public class ImpressionsController {

	private final XConnectionService xConnectionService;
	private final AppCache appCache;

	@GetMapping("/post/{postId}")
	public ResponseEntity<?> getImpressionsByPostId(@PathVariable("postId") Long postId,
						 @RequestHeader(value = "x-trace-id", required = false) String xTraceId,
						 @RequestHeader(value = "x-tenant-id", required = false) Long tenantId,
						 @AuthenticationPrincipal UserDetailsImpl userDetails) {

		try {
			ImpressionsResponse response = xConnectionService.getImpressionsByPostId(postId);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error | tweet-with-media | traceId={} | message={}", xTraceId, e.getMessage(), e);
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}

	}

	@PostMapping("/post")
	public ResponseEntity<?> createPost(@RequestHeader(value = "x-trace-id", required = true) String xTraceId,
									   @AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody  TweetRequest request) {
		try {
//			request.setFile(file);

			TweetResponse response = xConnectionService.createLinkedinPost(request,userDetails,null);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error | tweet-with-media | traceId={} | message={}", xTraceId, e.getMessage(), e);
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
		}
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
