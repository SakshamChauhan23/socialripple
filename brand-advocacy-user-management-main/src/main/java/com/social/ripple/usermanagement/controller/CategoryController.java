/**
 * Filename: CategoryController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.AssignCategoryRequest;
import com.social.ripple.usermanagement.dto.request.CategoryRequestDTO;
import com.social.ripple.usermanagement.dto.response.AssignCategoryResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.CategoryResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ICategoryService;
import com.social.ripple.usermanagement.util.ResponseUtils;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class CategoryController {

	private final ICategoryService categoryService;

	@Autowired
	public CategoryController(ICategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@PostMapping("/{version}/admin/categories")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<CategoryResponse> createCategory(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(value = "language-id", required = false) String languageId,
			@RequestBody CategoryRequestDTO requestDTO, @AuthenticationPrincipal UserDetailsImpl detailsImpl) {

		log.info("[{}]|CATEGORY|Controller|Create category request received", traceId);

		CategoryResponse response = categoryService.createCategory(traceId, correlationId, tenantId, languageId,
				requestDTO, detailsImpl);
		return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	}

	@PutMapping("/{version}/admin/categories/{id}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<CategoryResponse> updateCategory(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(value = "language-id", required = false) String languageId, @PathVariable Long id,
			@RequestBody CategoryRequestDTO requestDTO, @AuthenticationPrincipal UserDetailsImpl detailsImpl) {

		log.info("[{}]|CATEGORY|Controller|Update category request received for id: {}", traceId, id);

		CategoryResponse response = categoryService.updateCategory(traceId, correlationId, tenantId, languageId, id,
				requestDTO, detailsImpl);
		return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	}

	@GetMapping("/{version}/categories")
	public ResponseEntity<CategoryResponse> getAllCategories(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(value = "language-id", required = false) String languageId,
			@RequestParam(value = "id", required = false) Long id,
			@AuthenticationPrincipal UserDetailsImpl detailsImpl) {

		log.info("[{}]|CATEGORY|Controller|Get category request received", traceId);

		CategoryResponse response = categoryService.getAllCategories(traceId, correlationId, tenantId, languageId, id,
				detailsImpl);
		return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	}

	@DeleteMapping("/{version}/admin/categories/{id}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<CategoryResponse> deleteCategory(@PathVariable("version") String version,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(value = "language-id", required = false) String languageId, @PathVariable Long id,
			@AuthenticationPrincipal UserDetailsImpl detailsImpl) {

		log.info("[{}]|CATEGORY|Controller|Delete category request received for id: {}", traceId, id);

		CategoryResponse response = categoryService.deleteCategory(traceId, correlationId, tenantId, languageId, id,
				detailsImpl);
		return ResponseEntity.status(ResponseUtils.getHttpStatus(response.getCode())).body(response);
	}

	@PutMapping("/{version}/assign/category")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> assignCategoryToPost(@PathVariable("version") String version,
			@RequestBody AssignCategoryRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(name = "authorization", required = true) String authorization,
			@RequestHeader(value = "x-trace-id") String traceId, @RequestHeader(value = "x-tenant-id") String tenantId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "language-id", required = false) String languageId) {

		try {
			AssignCategoryResponse serviceResponse = categoryService.assignCategoryToPost(traceId, correlationId,
					tenantId, languageId, request.getPostId(), request, userDetails);

			if (serviceResponse != null) {
				serviceResponse.setTimestamp(new Date());
			}

			return ResponseEntity
					.status(resolveHttpStatus(
							serviceResponse != null ? serviceResponse.getCode() : ResponseCode.USMG_500))
					.body(serviceResponse);

		} catch (Exception e) {
			return buildErrorResponse("/posts/" + request.getPostId() + "/assign/category", ResponseCode.USMG_500,
					"Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
}
