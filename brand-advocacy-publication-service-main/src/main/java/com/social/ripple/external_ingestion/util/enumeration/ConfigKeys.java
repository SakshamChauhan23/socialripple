/**
 * Filename: ConfigKeys.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.external_ingestion.util.enumeration;

import java.util.Set;

public final class ConfigKeys {

	private ConfigKeys() {
	}

	// Mandatory Keys
	public static final String PASSWORD_ENCODING_DECODING_SECRET = "PASSWORD_ENCODING_DECODING_SECRET";
	public static final String JWT_SECRET = "JWT_SECRET";
	public static final String JWT_EXPIRATION_MS = "JWT_EXPIRATION_MS";
	public static final String JWT_REFRESH_TOKEN_SECRET = "JWT_REFRESH_TOKEN_SECRET";
	public static final String JWT_REFRESH_TOKEN_EXPIRATION_MS = "JWT_REFRESH_TOKEN_EXPIRATION_MS";
	public static final String RECAPTCHA_SECRET_KEY = "RECAPTCHA_SECRET_KEY";
	public static final String RECAPTCHA_VERIFY_URL = "RECAPTCHA_VERIFY_URL";
	public static final String INVITE_TOKEN_TTL_HOURS = "INVITE_TOKEN_TTL_HOURS";
	public static final String INVITE_SIGNUP_BASE_URL = "INVITE_SIGNUP_BASE_URL";
	public static final String GOOGLE_SSO_CLIENT_ID = "GOOGLE_SSO_CLIENT_ID";
	public static final String AZURE_AD_CLIENT_ID = "AZURE_AD_CLIENT_ID";
	public static final String AZURE_AD_JWKS_KEYS_ENDPOINT = "AZURE_AD_JWKS_KEYS_ENDPOINT";
	public static final String AZURE_AD_ISSUER = "AZURE_AD_ISSUER";
	public static final String JWT_RESET_TOKEN_SECRET = "JWT_RESET_TOKEN_SECRET";
	public static final String JWT_RESET_TOKEN_EXPIRATION_MS = "JWT_RESET_TOKEN_EXPIRATION_MS";
	public static final String NOTIFICATION_SERVICE_BASE_URL = "NOTIFICATION_SERVICE_BASE_URL";
	public static final String OTP_LENGTH = "OTP_LENGTH";
	public static final String MAX_REQUEST_SIZE = "OTP_EXPIRATION_MINUTES";
	public static final String FILE_SIZE_THRESHOLD = "FILE_SIZE_THRESHOLD";
	public static final String OTP_EXPIRATION_MINUTES = "OTP_EXPIRATION_MINUTES";
    public static final String X_OAUTH_URL = "X_OAUTH_URL";
	public static final Set<String> REQUIRED_KEYS = Set.of(PASSWORD_ENCODING_DECODING_SECRET, JWT_SECRET, JWT_EXPIRATION_MS, JWT_REFRESH_TOKEN_SECRET,
			JWT_REFRESH_TOKEN_EXPIRATION_MS, JWT_RESET_TOKEN_SECRET, JWT_RESET_TOKEN_EXPIRATION_MS, NOTIFICATION_SERVICE_BASE_URL, OTP_LENGTH,
			OTP_EXPIRATION_MINUTES, RECAPTCHA_SECRET_KEY, INVITE_TOKEN_TTL_HOURS, INVITE_SIGNUP_BASE_URL, GOOGLE_SSO_CLIENT_ID, AZURE_AD_CLIENT_ID,
			AZURE_AD_JWKS_KEYS_ENDPOINT, AZURE_AD_ISSUER, X_OAUTH_URL);

	public static final String MEDIA_FILE_STORAGE_PATH = "MEDIA_FILE_STORAGE_PATH";
	public static final String MEDIA_FILE_STORAGE_BASE_URL = "MEDIA_FILE_STORAGE_BASE_URL";
	public static final String MEDIA_IMAGE_PROVIDER = "MEDIA_IMAGE_PROVIDER";
	public static final String MEDIA_VIDEO_PROVIDER = "MEDIA_VIDEO_PROVIDER";
	public static final String BUNNY_STORAGE_ZONE = "BUNNY_STORAGE_ZONE";
	public static final String BUNNY_STORAGE_REGION = "BUNNY_STORAGE_REGION";
	public static final String BUNNY_STORAGE_ACCESS_KEY = "BUNNY_STORAGE_ACCESS_KEY";
	public static final String BUNNY_PULL_ZONE_BASE_URL = "BUNNY_PULL_ZONE_BASE_URL";
	public static final String MAX_FILES_IN_ZIP = "MAX_FILES_IN_ZIP";
	public static final String MAX_FILE_SIZE = "MAX_FILE_SIZE";
	public static final String MAX_FILE_SIZE_MB = "MAX_FILE_SIZE_MB";

	public static final String GEMINI_API_KEY = "GEMINI_API_KEY";
	public static final String GEMINI_API_URL = "GEMINI_API_URL";
}
