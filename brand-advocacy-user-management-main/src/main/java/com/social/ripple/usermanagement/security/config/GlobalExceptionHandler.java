/**
 * Filename: GlobalExceptionHandler.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.security.config;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<BaseResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {

		log.info("USMG|Maximum size exeed|failed|{}", ex.getMessage());
		String message = "File size exceeds maximum allowed limit ";

		ErrorObj error = new ErrorObj(request.getRequestURI(), ResponseCode.USMG_413, ex.getClass().getSimpleName(), message);

		BaseResponse errorResponse = new BaseResponse(false, ResponseCode.USMG_413, "File size too large", message, new Date(), List.of(error));

		return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<BaseResponse> handleMultipartException(MultipartException ex, HttpServletRequest request) {
		log.info("USMG|Multipart exception|failed|{}", ex.getMessage());
		String message = "File upload failed";

		if (ex.getCause() != null) {
			message = ex.getCause().getMessage();
			if (ex.getCause().getCause() != null) {
				message = ex.getCause().getCause().getMessage();
			}
		}

		ErrorObj error = new ErrorObj(request.getRequestURI(), ResponseCode.USMG_413, ex.getClass().getSimpleName(), message);

		BaseResponse errorResponse = new BaseResponse(false, ResponseCode.USMG_413, "File upload error", message, new Date(), List.of(error));

		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}
}
