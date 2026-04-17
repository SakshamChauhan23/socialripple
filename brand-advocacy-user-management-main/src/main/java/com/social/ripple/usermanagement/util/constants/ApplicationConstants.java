/**
 * Filename: ApplicationConstants.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.util.constants;

public class ApplicationConstants {

	private ApplicationConstants() {
	}

	public static final String API_VERSION = "/v1";
	public static final String AUTH_TOKEN_FILTER_API_KEY = "x-api-key";
	public static final String PASSWORD_ENCODING_ALGORITHM = "AES/GCM/NoPadding";
	public static final String USER_ROLE = "ROLE_EMPLOYEE";
	public static final String USER_LOCKED_STATUS = "LOCKED";
	public static final String USER_ACTIVE_STATUS = "ACTIVE";
	public static final String USER_INVITED_STATUS = "INVITED";
	public static final String USER_INVITATION_EXPIRED = "INVITATION_EXPIRED";
	public static final String MEDIA_FILE_STORAGE_BASE_URL = "media.file.storage.base.url";
	public static final String MEDIA_FILE_STORAGE_PATH = "F:\\advocacy\\Media_Storage\\";
	public static final String SSO_PAYLOAD_NAME = "name";
	public static final String SSO_PAYLOAD_PICTURE = "picture";
	public static final String SSO_PAYLOAD_LOCALE = "locale";
	public static final String SSO_PAYLOAD_GIVEN_NAME = "given_name";
	public static final String SSO_PAYLOAD_FAMILY_NAME = "family_name";
	public static final String GOOGLE_SSO_PROVIDER = "google";
	public static final String MICROSOFT_SSO_PROVIDER = "microsoft";
	public static final String ACTIVE_USER_STATUS = "ACTIVE";
	public static final String INACTIVE_USER_STATUS = "INACTIVE";
	public static final String INVITED_USER_STATUS = "INVITED";
	public static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
	public static final String ADMIN_ROLE = "ROLE_ADMIN";
	public static final String OTP_NUMERIC_CHARACTERS = "0123456789";
	public static final String[] ALLOWED_IMAGE_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".webp" };
	public static final String[] ALLOWED_VIDEO_EXTENSIONS = { ".mp4", ".mov", ".avi" };
	public static final String USER_ENABLED_STATUS = "ENABLED";
	public static final String USER_DISABLED_STATUS = "DISABLED";

	public static final String ROLE_ADMIN="ROLE_ADMIN";

}
