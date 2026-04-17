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
package com.social.ripple.notification_service.util;

import java.util.Set;

public final class ConfigKeys {

	private ConfigKeys() {
	}

	public static final String SMTP_HOST = "SMTP_HOST";
	public static final String SMTP_PORT = "SMTP_PORT";
	public static final String MAIL_ID = "MAIL_ID";
	public static final String SMTP_MAIL_PASSWORD = "SMTP_MAIL_PASSWORD";
	public static final String MAIL_TRANSPORT_PROTOCOL = "MAIL_TRANSPORT_PROTOCOL";
	public static final String MAIL_SMTP_AUTH = "MAIL_SMTP_AUTH";
	public static final String MAIL_SMTP_STARTTLS_ENABLE = "MAIL_SMTP_STARTTLS_ENABLE";
	public static final String MAIL_DEBUG = "MAIL_DEBUG";
	public static final String OTP_MAIL_BODY = "OTP_MAIL_BODY";
	public static final String INVITATION_LINK_MAIL_BODY = "INVITATION_LINK_MAIL_BODY";

	// AWS SES
	public static final String AWS_SES_REGION = "AWS_SES_REGION";
	public static final String AWS_SES_ACCESS_KEY_ID = "AWS_SES_ACCESS_KEY_ID";
	public static final String AWS_SES_SECRET_ACCESS_KEY = "AWS_SES_SECRET_ACCESS_KEY";

	public static final Set<String> REQUIRED_KEYS = Set.of(MAIL_ID, OTP_MAIL_BODY, INVITATION_LINK_MAIL_BODY,
			AWS_SES_REGION, AWS_SES_ACCESS_KEY_ID, AWS_SES_SECRET_ACCESS_KEY);

}
