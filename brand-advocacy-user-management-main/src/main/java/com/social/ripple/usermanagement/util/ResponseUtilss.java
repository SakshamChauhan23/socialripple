/**
 * Filename: ResponseUtilss.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.usermanagement.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.LoyaltyResponse;

public class ResponseUtilss {
	 
	public static HttpStatus getHttpStatus(String code) {
		if (code == null)
			return HttpStatus.INTERNAL_SERVER_ERROR;
 
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
 
	public static ResponseEntity<?> buildResponse(LoyaltyResponse response) {
		if (response.getCode() == null)
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
 
		switch (response.getCode()) {
			case "USMG_200":
				return ResponseEntity.ok(response);
			case "USMG_400":
				return ResponseEntity.badRequest().body(response);
			case "USMG_401":
			case "USMG_403":
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
			case "USMG_404":
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			default:
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
 
	}

}
