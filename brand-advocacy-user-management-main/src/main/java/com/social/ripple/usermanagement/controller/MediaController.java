
/**
 * Filename: MediaController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.social.ripple.usermanagement.dto.request.CreatePostRequest;
import com.social.ripple.usermanagement.dto.request.UpdateMediaLibraryRequest;
import com.social.ripple.usermanagement.dto.request.UploadLibraryRequest;
import com.social.ripple.usermanagement.dto.request.ZipUploadRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IMediaService;
import com.social.ripple.usermanagement.util.ResponseUtils;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@Slf4j
public class MediaController {

	private final IMediaService mediaService;

	public MediaController(IMediaService mediaService) {
		this.mediaService = mediaService;
	}

	@PostMapping("{version}/media/upload")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> uploadMedia(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestParam("files") MultipartFile[] files, @RequestParam("mediaType") String mediaType) {
		try {
			BaseResponse response = mediaService.uploadMedia(traceId, userDetails, files, mediaType);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		} catch (Exception e) {
			return buildErrorResponse("media/upload", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("{version}/media/post")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> postMedia(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestBody CreatePostRequest post) {
		try {
			BaseResponse response = mediaService.postdMedia(traceId, userDetails, post);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		} catch (Exception e) {
			return buildErrorResponse("media/post", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("{version}/media/list")
	@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> getMedia(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
			@RequestParam(value = "postId", required = false) Long postId,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@RequestParam(value = "searchText", required = false) String searchText,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@RequestParam(value = "leader", required = false, defaultValue = "false") boolean leader) {
		try {
			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = mediaService.getMedia(traceId, postId, userDetails, pageable, tenantId, categoryId,
					searchText, leader);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return buildErrorResponse("media/list", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("{version}/media/upload-zip")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> uploadMediaFromZip(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestParam("zipFile") MultipartFile zipFile, @RequestParam("mediaType") String mediaType) {
		try {
			ZipUploadRequest zipUploadRequest = new ZipUploadRequest();
			zipUploadRequest.setZipFile(zipFile);
			zipUploadRequest.setMediaType(mediaType);

			BaseResponse response = mediaService.unzipFile(traceId, tenantId, userDetails, zipUploadRequest);
			response.setTimestamp(new Date());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return buildErrorResponse("media/upload-zip", ResponseCode.USMG_500, "Internal server error",
					e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

		}
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String path, String code, String error, String message,
			HttpStatus status) {
		ErrorObj errorObj = new ErrorObj(path, code, error, message);
		BaseResponse baseResponse = new BaseResponse();
		baseResponse.setStatus(false);
		baseResponse.setCode(code);
		baseResponse.setMessage(error);
		baseResponse.setDevMessage(message);
		baseResponse.setTimestamp(new Date());
		baseResponse.setErrors(List.of(errorObj));
		return new ResponseEntity<>(baseResponse, status);
	}

	private HttpStatus resolveHttpStatus(String code) {
		return switch (code) {
		case ResponseCode.USMG_400 -> HttpStatus.BAD_REQUEST;
		case ResponseCode.USMG_401 -> HttpStatus.UNAUTHORIZED;
		case ResponseCode.USMG_403 -> HttpStatus.FORBIDDEN;
		case ResponseCode.USMG_404 -> HttpStatus.NOT_FOUND;
		case ResponseCode.USMG_409 -> HttpStatus.CONFLICT;
		case ResponseCode.USMG_422 -> HttpStatus.UNPROCESSABLE_ENTITY;
		case ResponseCode.USMG_423 -> HttpStatus.LOCKED;
		case ResponseCode.USMG_429 -> HttpStatus.TOO_MANY_REQUESTS;
		case ResponseCode.USMG_500 -> HttpStatus.INTERNAL_SERVER_ERROR;
		case ResponseCode.USMG_503 -> HttpStatus.SERVICE_UNAVAILABLE;
		case ResponseCode.USMG_504 -> HttpStatus.GATEWAY_TIMEOUT;
		default -> HttpStatus.OK;
		};
	}

	@PostMapping("{version}/media/uploadlibrary")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> uploadMediaLibrary(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestBody UploadLibraryRequest libraryRequest) {
		try {
			BaseResponse response = mediaService.addMediaToLibrary(traceId, tenantId, userDetails, libraryRequest);
			response.setTimestamp(new Date());
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return buildErrorResponse("media/library", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	  /**
     * Employee + Admin → ONLY ACTIVE media (default behavior)
     */
    @GetMapping("{version}/media/library")
    @PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<BaseResponse> fetchMediaLibrary(
            @PathVariable("version") String version,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "x-trace-id", required = true) String traceId,
            @RequestHeader(value = "x-tenant-id", required = true) String tenantId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "language-id", required = false) String languageId,
            @RequestParam(value = "mediaType", required = true) String mediaType,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            log.info("[{}]|LIBRARY|Controller|fetchMediaLibrary|Employee/Admin (ACTIVE only) called", traceId);
            BaseResponse response = mediaService.fetchMediaLibrary(traceId, tenantId, userDetails, mediaType, categoryId, page, size);
            response.setTimestamp(new Date());
            return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
        } catch (Exception e) {
            log.error("[{}]|LIBRARY|Controller|fetchMediaLibrary|ERROR {}", traceId, e.getMessage(), e);
            BaseResponse errorResponse = new BaseResponse();
            errorResponse.setStatus(false);
            errorResponse.setCode(ResponseCode.USMG_500);
            errorResponse.setMessage("Internal server error");
            errorResponse.setDevMessage(e.getMessage());
            errorResponse.setTimestamp(new Date());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Admin Only → ACTIVE (default) or ARCHIVED based on "status" param
     */
    @GetMapping("{version}/admin/media/library")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BaseResponse> fetchMediaLibraryAdmin(
            @PathVariable("version") String version,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestHeader(value = "x-trace-id", required = true) String traceId,
            @RequestHeader(value = "x-tenant-id", required = true) String tenantId,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "language-id", required = false) String languageId,
            @RequestParam(value = "mediaType", required = true) String mediaType,
            @RequestParam(value = "status", required = false, defaultValue = "active") String status, // "active" or "archived"
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        try {
            log.info("[{}]|LIBRARY|Controller|fetchMediaLibraryAdmin|Admin called with status={}", traceId, status);
            BaseResponse response = mediaService.fetchMediaLibraryAdmin(traceId, tenantId, userDetails, mediaType, status, categoryId, page, size);
            response.setTimestamp(new Date());
            return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
        } catch (Exception e) {
            log.error("[{}]|LIBRARY|Controller|fetchMediaLibraryAdmin|ERROR {}", traceId, e.getMessage(), e);
            BaseResponse errorResponse = new BaseResponse();
            errorResponse.setStatus(false);
            errorResponse.setCode(ResponseCode.USMG_500);
            errorResponse.setMessage("Internal server error");
            errorResponse.setDevMessage(e.getMessage());
            errorResponse.setTimestamp(new Date());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
	@PutMapping("{version}/media/updateLibrary")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> updateMediaLibrary(@PathVariable("version") String version,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestBody UpdateMediaLibraryRequest updateLibraryRequest) {
		try {
			BaseResponse response = mediaService.updateMediaLibrary(traceId, tenantId, userDetails,
					updateLibraryRequest);
			response.setTimestamp(new Date());
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			BaseResponse errorResponse = new BaseResponse();
			errorResponse.setStatus(false);
			errorResponse.setCode(ResponseCode.USMG_500);
			errorResponse.setMessage("Internal server error");
			errorResponse.setDevMessage(e.getMessage());
			errorResponse.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

		}

	}

	@DeleteMapping("{version}/media/{libraryId}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> updateMediaLibrary(@PathVariable("version") String version,
			@PathVariable Long libraryId, @AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId) {
		try {
			BaseResponse response = mediaService.deleteMediaLibrary(traceId, tenantId, userDetails, libraryId);
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			BaseResponse errorResponse = new BaseResponse();
			errorResponse.setStatus(false);
			errorResponse.setCode("USMG_500");
			errorResponse.setMessage("Unhandled error during media library deletion");
			errorResponse.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}
}
