/**
 * Filename: AppCommonValidator.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.util;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.social.ripple.usermanagement.util.constants.ApplicationConstants;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppCommonValidator {

	public boolean isNullOrEmpty(String traceId, String input, String fieldName) {
		boolean result = input == null || input.trim().isEmpty();

		if (result) {
			log.warn("[{}] Validation failed: '{}' is null or empty | input: {}", traceId, fieldName, input);
		}

		return result;
	}

	public boolean isValidEmail(String traceId, String email) {
		boolean isValid = email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

		if (!isValid) {
			log.warn("[{}] Validation failed: Invalid email format: {}", traceId, email);
		}
		return isValid;
	}

	public boolean isNumeric(String traceId, String input, String fieldName) {
		try {
			Long.parseLong(input);
			return true;
		}
		catch (NumberFormatException e) {
			log.warn("[{}] Validation failed: '{}' is not a valid numeric value: {}", traceId, fieldName, input);
			return false;
		}
	}

	public boolean isValidPhoneNumber(String traceId, String phone) {
		boolean isValid = phone != null && phone.matches("^[0-9]{10,15}$");

		if (!isValid) {
			log.warn("[{}] Validation failed: Invalid phone number: {}", traceId, phone);
		}
		return isValid;
	}

	public String getOrGenerateRequestId(String requestId) {
		if (requestId != null && !requestId.isBlank()) {
			log.debug("Using provided requestId: {}", requestId);
			return requestId;
		}
		else {
			String generatedId = UUID.randomUUID().toString();
			log.info("Generated new requestId: {}", generatedId);
			return generatedId;
		}
	}

	public String getOrGenerateRequestId(HttpServletRequest request) {
		return getOrGenerateRequestId(request.getHeader("x-trace-id"));
	}

	public boolean isValidPassword(String traceId, String password, String fieldName) {
		if (password == null) {
			log.warn("[{}] Validation failed: '{}' is null", traceId, fieldName);
			return false;
		}

		boolean lengthValid = password.length() >= 6 && password.length() <= 16;
		boolean hasUppercase = password.matches(".*[A-Z].*");
		boolean hasLowercase = password.matches(".*[a-z].*");
		boolean hasDigit = password.matches(".*\\d.*");
		boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

		boolean isValid = lengthValid && hasUppercase && hasLowercase && hasDigit && hasSpecialChar;

		if (!isValid) {
			log.warn(
					"[{}] Validation failed: '{}' does not meet password policy. "
							+ "LengthValid={}, Uppercase={}, Lowercase={}, Digit={}, SpecialChar={}, Input={}",
					traceId, fieldName, lengthValid, hasUppercase, hasLowercase, hasDigit, hasSpecialChar, password);
		}

		return isValid;
	}

	public boolean hasSpecialCharacters(String traceId, String input, String fieldName) {
		boolean result = !input.matches("^[a-zA-Z0-9\\s]+$");
		if (result) {
			log.warn("[{}] Validation failed: '{}' contains special characters | input: {}", traceId, fieldName, input);
		}
		return result;
	}

	public boolean isLengthInvalid(String traceId, String input, String fieldName, int min, int max) {
		boolean result = input == null || input.length() < min || input.length() > max;
		if (result) {
			log.warn("[{}] Validation failed: '{}' must be between {} and {} characters | input: {}", traceId, fieldName, min, max, input);
		}
		return result;
	}

	public String generateNumericOtp(String traceId, int length) {
		try {
			if (length <= 0) {
				log.error("[{}]|OTP_GENERATE|FAILED|Invalid OTP length: {}", traceId, length);
				return null;
			}

			SecureRandom random = new SecureRandom();
			StringBuilder sb = new StringBuilder(length);

			for (int i = 0; i < length; i++) {
				sb.append(ApplicationConstants.OTP_NUMERIC_CHARACTERS.charAt(random.nextInt(ApplicationConstants.OTP_NUMERIC_CHARACTERS.length())));
			}

			String otp = sb.toString();
			log.info("[{}]|OTP_GENERATE|SUCCESS|Generated OTP of length {}", traceId, length);
			return otp;

		}
		catch (Exception e) {
			log.error("[{}]|OTP_GENERATE|ERROR|Failed to generate OTP: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	public boolean isValidEmailFormat(String traceId, String email) {
		if (email == null || email.isBlank())
			return false;
		String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
		boolean isValid = email.matches(emailRegex);

		log.debug("[{}]|Validator|EmailFormat|{}|Result: {}", traceId, email, isValid);
		return isValid;
	}

	public static boolean isValidMobileNumber(String phone) {
		if (!StringUtils.hasText(phone)) {
			return false;
		}

		String trimmedPhone = phone.trim();

		String indiaPattern = "^(\\+91)?[6-9]\\d{9}$";
		String usPattern = "^(\\+1)?\\d{10}$";
		String ukPattern = "^(\\+44)?7\\d{9}$";
		String uaePattern = "^(\\+971)?5\\d{8}$";
		String iranPattern = "^(\\+98)?9\\d{9}$";
		String saPattern = "^(\\+27)?[6-8]\\d{8}$";
		String nigeriaPattern = "^(\\+234)?[789]\\d{9}$";
		String germanyPattern = "^(\\+49)?1[5-7]\\d{8}$";
		String francePattern = "^(\\+33)?6\\d{8}$";

		return Pattern.matches(indiaPattern, trimmedPhone) || Pattern.matches(usPattern, trimmedPhone) || Pattern.matches(ukPattern, trimmedPhone)
				|| Pattern.matches(uaePattern, trimmedPhone) || Pattern.matches(iranPattern, trimmedPhone) || Pattern.matches(saPattern, trimmedPhone)
				|| Pattern.matches(nigeriaPattern, trimmedPhone) || Pattern.matches(germanyPattern, trimmedPhone)
				|| Pattern.matches(francePattern, trimmedPhone);
	}

	public static boolean isValidUsername(String username) {
		if (!StringUtils.hasText(username)) {
			return false;
		}

		String trimmed = username.trim();
		String usernamePattern = "^[a-zA-Z0-9](?!.*[._]{2})[a-zA-Z0-9._]{0,28}[a-zA-Z0-9]$";

		return Pattern.matches(usernamePattern, trimmed);
	}

}
