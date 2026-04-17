/**
 * Filename: PasswordRecoveryServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.social.ripple.usermanagement.dao.model.OtpStore;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.repository.OtpStoreRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.ForgetPasswordRequest;
import com.social.ripple.usermanagement.dto.ResetPasswordRequest;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.ForgetPasswordResponse;
import com.social.ripple.usermanagement.dto.response.PasswordResponseDTO;
import com.social.ripple.usermanagement.dto.response.ResetPasswordResponse;
import com.social.ripple.usermanagement.security.jwt.JwtUtils;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ResponseCode;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PasswordRecoveryServiceImpl implements IPasswordRecoveryService {

	private JwtUtils jwtUtils;
	private UserRepository userRepository;
	private AppCommonValidator appCommonValidator;
	private OtpStoreRepository otpStoreRepository;
	private OtpService otpService;
	private PasswordEncoder passwordEncoder;

	@Autowired
	public PasswordRecoveryServiceImpl(JwtUtils jwtUtils, UserRepository userRepository, AppCommonValidator appCommonValidator,
			OtpStoreRepository otpStoreRepository, OtpService otpService, PasswordEncoder passwordEncoder) {
		this.jwtUtils = jwtUtils;
		this.userRepository = userRepository;
		this.appCommonValidator = appCommonValidator;
		this.otpStoreRepository = otpStoreRepository;
		this.otpService = otpService;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public ForgetPasswordResponse forgetpasword(String traceId, String xCorrelationId, String tenantId, String languageId,
			ForgetPasswordRequest request) {

		log.info("[{}]|FORGET_PASSWORD|START|Email: {}", traceId, request.getEmail());
		ForgetPasswordResponse resp = new ForgetPasswordResponse();
		resp.setTimestamp(new Date());

		try {
			User user = userRepository.findByEmail(request.getEmail());
			if (user == null) {
				log.warn("[{}]|FORGET_PASSWORD|WARN|User not found for email: {}", traceId, request.getEmail());
				return buildError(resp, ResponseCode.USMG_404, "User not found", "USER_NOT_FOUND", null);
			}

			String resetToken = jwtUtils.generateResetToken(traceId, user.getEmail());
			if (resetToken == null) {
				log.error("[{}]|FORGET_PASSWORD|ERROR|Failed to generate reset token for email: {}", traceId, user.getEmail());
				return buildError(resp, ResponseCode.USMG_500, "Failed to generate reset token", "TOKEN_ERROR", null);
			}

			boolean otpSent = otpService.generateAndSendOtp(traceId, user.getEmail(), "PASSWORD_RESET");

			if (!otpSent) {
				log.error("[{}]|FORGET_PASSWORD|ERROR|Failed to send OTP for email: {}", traceId, user.getEmail());

				ErrorObj error = new ErrorObj();
				error.setCode("OTP_SEND_ERROR");
				error.setMessage("Failed to send OTP due to notification service error");
				return buildError(resp, ResponseCode.USMG_500, "Failed to send OTP", "OTP_SEND_ERROR", List.of(error));
			}

			log.info("[{}]|FORGET_PASSWORD|INFO|OTP generated and sent via notification service for email: {}", traceId, user.getEmail());

			PasswordResponseDTO data = new PasswordResponseDTO();
			data.setResetToken(resetToken);
			log.info("[{}]|FORGET_PASSWORD|SUCCESS|Request processed successfully for email: {}", traceId, request.getEmail());
			return buildSuccess(resp, ResponseCode.USMG_200, "Request processed successfully.", null, data);

		}
		catch (Exception e) {
			log.error("[{}]|FORGET_PASSWORD|ERROR|Exception during forget password for email: {}: {}", traceId, request.getEmail(), e.getMessage(),
					e);
			return buildError(resp, ResponseCode.USMG_500, "Internal server error", "SERVER_ERROR", null);
		}
	}

	private ForgetPasswordResponse buildSuccess(ForgetPasswordResponse resp, String code, String message, String devMsg, PasswordResponseDTO data) {
		resp.setStatus(true);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(devMsg);
		resp.setErrors(null);
		resp.setData(data);
		return resp;
	}

	private ForgetPasswordResponse buildError(ForgetPasswordResponse resp, String code, String message, String devMsg, List<ErrorObj> errors) {
		resp.setStatus(false);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(devMsg);
		resp.setErrors(errors);
		return resp;
	}

	private ResetPasswordResponse buildError(ResetPasswordResponse resp, String code, String message, String devMsg, List<ErrorObj> errors) {
		resp.setStatus(false);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(devMsg);
		resp.setErrors(errors);
		return resp;
	}

	private ResetPasswordResponse buildSuccess(ResetPasswordResponse resp, String code, String message) {
		resp.setStatus(true);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(null);
		resp.setErrors(null);
		return resp;
	}

	@Override
	public ResetPasswordResponse resetpasword(String xTraceId, String xCorrelationId, String tenantId, String languageId,
			ResetPasswordRequest passwordRequest) {

		log.info("[{}]|RESET_PASSWORD|START|ResetToken: {}", xTraceId, passwordRequest.getResetToken());
		ResetPasswordResponse response = new ResetPasswordResponse();

		boolean missingField = appCommonValidator.isNullOrEmpty(xTraceId, passwordRequest.getResetToken(), "reset token")
				|| appCommonValidator.isNullOrEmpty(xTraceId, passwordRequest.getOtp(), "otp")
				|| appCommonValidator.isNullOrEmpty(xTraceId, passwordRequest.getNewPassword(), "new password")
				|| appCommonValidator.isNullOrEmpty(xTraceId, passwordRequest.getConfirmNewPassword(), "confirm password");

		if (missingField) {
			log.warn("[{}]|RESET_PASSWORD|WARN|One or more required fields are null or empty.", xTraceId);
			return buildError(response, ResponseCode.USMG_400, "Missing required parameter", "One or more fields are null or empty", null);
		}

		if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmNewPassword())) {
			log.warn("[{}]|RESET_PASSWORD|WARN|New password and confirm password do not match.", xTraceId);
			return buildError(response, ResponseCode.USMG_400, "Passwords do not match", "newPassword and confirmNewPassword must be identical",
					null);
		}

		if (!appCommonValidator.isValidPassword(xTraceId, passwordRequest.getNewPassword(), "newPassword")) {
			log.warn("[{}]|RESET_PASSWORD|WARN|New password does not meet policy requirements.", xTraceId);
			return buildError(response, ResponseCode.USMG_400, "Password does not meet policy requirements",
					"Password must be 6-16 characters, include uppercase, lowercase, digit, and special character", null);
		}

		String email = jwtUtils.getSubjectFromResetToken(xTraceId, passwordRequest.getResetToken());
		if (email == null) {
			log.warn("[{}]|RESET_PASSWORD|WARN|Invalid or expired reset token. Cannot extract user information.", xTraceId);
			return buildError(response, ResponseCode.USMG_401, "Invalid token", "Cannot extract user information from token", null);
		}
		log.info("[{}]|RESET_PASSWORD|INFO|Extracted email from token: {}", xTraceId, email);

		User user = userRepository.findByEmail(email);
		if (user == null) {
			log.warn("[{}]|RESET_PASSWORD|WARN|User not found for email: {}", xTraceId, email);
			return buildError(response, ResponseCode.USMG_404, "User not found", "No user found for email: " + email, null);
		}

		Optional<OtpStore> otpOptional = otpStoreRepository.findLatestValidOtp(email, "PASSWORD_RESET", LocalDateTime.now());

		if (!otpOptional.isPresent()) {
			log.warn("[{}]|RESET_PASSWORD|WARN|No valid OTP found for email: {}.", xTraceId, email);
			return buildError(response, ResponseCode.USMG_401, "Invalid OTP", "No valid OTP found for this user", null);
		}

		OtpStore otpStore = otpOptional.get();

		if (!otpStore.getOtpCode().equals(passwordRequest.getOtp())) {
			log.warn("[{}]|RESET_PASSWORD|WARN|Provided OTP does not match for email: {}.", xTraceId, email);
			return buildError(response, ResponseCode.USMG_401, "Invalid OTP", "OTP does not match", null);
		}

		if (otpStore.getExpiresAt().isBefore(LocalDateTime.now())) {
			log.warn("[{}]|RESET_PASSWORD|WARN|OTP has expired for email: {}.", xTraceId, email);
			return buildError(response, ResponseCode.USMG_401, "Expired OTP", "OTP has expired", null);
		}

		if (passwordEncoder.matches(passwordRequest.getNewPassword(), user.getPasswordHash())) {
			log.warn("[{}]|RESET_PASSWORD|WARN|New password is same as current password for email: {}", xTraceId, email);
			return buildError(response, ResponseCode.USMG_400, "PASSWORD_REUSE_NOT_ALLOWED",
					"New password must be different from the current password", null);
		}

		otpStore.setIsUsed(true);
		otpStoreRepository.save(otpStore);
		log.info("[{}]|RESET_PASSWORD|INFO|OTP marked as used for email: {}", xTraceId, email);

		user.setPasswordHash(passwordEncoder.encode(passwordRequest.getNewPassword()));
		userRepository.save(user);
		log.info("[{}]|RESET_PASSWORD|SUCCESS|Password reset successfully for email: {}", xTraceId, user.getEmail());
		return buildSuccess(response, ResponseCode.USMG_200, "Password reset successfully");
	}
}