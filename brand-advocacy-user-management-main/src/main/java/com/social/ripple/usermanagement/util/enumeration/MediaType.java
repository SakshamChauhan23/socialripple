/**
 * Filename: MediaType.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.util.enumeration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MediaType {
	IMAGE, VIDEO;

	public static MediaType fromString(String type) {
		if (type == null) {
			log.warn("MEDIA_TYPE|NULL_INPUT|Provided media type is null.");
			return null;
		}
		try {
			return switch (type.trim().toUpperCase()) {
				case "IMAGE", "IMG", "JPEG", "PNG" -> IMAGE;
				case "VIDEO", "MP4", "MOV" -> VIDEO;
				default -> throw new IllegalArgumentException("Unknown media type: " + type);
			};
		}
		catch (IllegalArgumentException ex) {
			log.warn("MEDIA_TYPE|INVALID_INPUT|Unsupported media type: '{}'", type);
			return null;
		}
	}
}
