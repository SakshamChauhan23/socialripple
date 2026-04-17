/**
 * Filename: ResponseCode.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.external_ingestion.constants;

/**
 * Centralized standard error codes used across the application. Format:
 * MODULE_HTTPSTATUS Example: AUTH_401 → Authentication module, Unauthorized
 */
public final class ResponseCode {

	private ResponseCode() {

	}

	// Success codes
	public static final String MS_200 = "MS_200"; // OK
	public static final String MS_201 = "MS_201"; // Created
	public static final String MS_202 = "MS_202"; // Accepted
	public static final String MS_204 = "MS_204"; // No Content

	// Client error codes
	public static final String MS_400 = "MS_400"; // Bad Request
	public static final String MS_401 = "MS_401"; // Unauthorized
	public static final String MS_403 = "MS_403"; // Forbidden
	public static final String MS_404 = "MS_404"; // Not Found
	public static final String MS_409 = "MS_409"; // Conflict
	public static final String MS_413 = "MS_413"; // Payload Too Large
	public static final String MS_422 = "MS_422"; // Unprocessable Entity
	public static final String MS_423 = "MS_423"; // Locked
	public static final String MS_429 = "MS_429"; // Too Many Requests

	// Server error codes
	public static final String MS_500 = "MS_500"; // Internal Server Error
	public static final String MS_503 = "MS_503"; // Service Unavailable
	public static final String MS_504 = "MS_504"; // Gateway Timeout

	// Media Service specific codes (optional, extendable)
	public static final String MS_AUTH_401 = "MS_AUTH_401"; // Authentication failure
	public static final String MS_TOKEN_EXPIRED = "MS_TOKEN_440"; // Custom: token expired
	public static final String MS_INVALID_TRANSACTION = "MS_TX_404"; // Transaction not found

}
