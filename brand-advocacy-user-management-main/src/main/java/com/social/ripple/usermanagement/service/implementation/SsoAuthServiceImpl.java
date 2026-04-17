/**
 * Filename: SsoAuthServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.InvitationToken;
import com.social.ripple.usermanagement.dao.model.Organization;
import com.social.ripple.usermanagement.dao.model.Role;
import com.social.ripple.usermanagement.dao.model.SsoProvider;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserRole;
import com.social.ripple.usermanagement.dao.repository.InvitationTokenRepository;
import com.social.ripple.usermanagement.dao.repository.RoleRepository;
import com.social.ripple.usermanagement.dao.repository.SsoProviderRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.UserRoleRepository;
import com.social.ripple.usermanagement.dto.GoogleUserDto;
import com.social.ripple.usermanagement.dto.MicrosoftUserDto;
import com.social.ripple.usermanagement.dto.request.SsoAuthRequest;
import com.social.ripple.usermanagement.dto.response.AuthResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.SsoAuthResponse;
import com.social.ripple.usermanagement.security.config.PasswordEncryptionDecryption;
import com.social.ripple.usermanagement.security.jwt.JwtUtils;
import com.social.ripple.usermanagement.service.ISsoAuthService;
import com.social.ripple.usermanagement.service.ISsoTokenValidatorService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SsoAuthServiceImpl implements ISsoAuthService {

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final RoleRepository roleRepository;
	private final InvitationTokenRepository invitationTokenRepository;
	private final SsoProviderRepository ssoProviderRepository;
	private final ISsoTokenValidatorService ssoTokenValidatorService;
	private final PasswordEncryptionDecryption passwordEncryptionDecryption;
	private final JwtUtils jwtTokenService;
	private final AppCommonValidator validator;

	public SsoAuthServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository, RoleRepository roleRepository,
			InvitationTokenRepository invitationTokenRepository, SsoProviderRepository ssoProviderRepository,
			ISsoTokenValidatorService ssoTokenValidatorService, PasswordEncryptionDecryption passwordEncryptionDecryption, JwtUtils jwtTokenService,
			AppCommonValidator validator) {
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.roleRepository = roleRepository;
		this.invitationTokenRepository = invitationTokenRepository;
		this.ssoProviderRepository = ssoProviderRepository;
		this.ssoTokenValidatorService = ssoTokenValidatorService;
		this.passwordEncryptionDecryption = passwordEncryptionDecryption;
		this.jwtTokenService = jwtTokenService;
		this.validator = validator;
	}

	@Override
	public BaseResponse ssoRegister(String traceId, SsoAuthRequest request) {
		traceId = validator.getOrGenerateRequestId(traceId);
		String method = "/sso/register";
		log.info("[{}]|SSO_REGISTER|STARTED|Payload: {}", traceId, request);

		try {
			if (isInvalidSigiInRequest(request, traceId)) {
				return buildError(ResponseCode.USMG_400, "Invalid request payload", traceId, method, "BadRequest",
						"Missing required fields or null values");
			}

			SsoProvider provider = validateSsoProvider(traceId, request.getProviderId());
			if (provider == null) {
				return buildError(ResponseCode.USMG_400, "Invalid or disabled SSO provider", traceId, method, "InvalidProvider",
						"SSO provider not found or disabled");
			}

			String ssoEmail = validateSsoTokenAndExtractEmail(traceId, provider, request.getSsoToken());
			if (ssoEmail == null) {
				return buildError(ResponseCode.USMG_401, provider.getName() + " SSO token validation failed", traceId, method, "InvalidToken",
						"Token validation failed or email not verified");
			}

			User existingUser = userRepository.findByEmail(ssoEmail);
			if (existingUser != null) {
				if (existingUser.getOrganization() == null && hasRole(existingUser.getId(), ApplicationConstants.ADMIN_ROLE)) {
					String accessToken = jwtTokenService.generateAccessToken(traceId, existingUser.getEmail());
					String refreshToken = jwtTokenService.generateRefreshToken(traceId, existingUser.getEmail());
					return createSsoAuthSuccessResponse(ResponseCode.USMG_200, accessToken, refreshToken, existingUser);
				}
				return buildError(ResponseCode.USMG_409, "User already exists", traceId, method, "UserAlreadyExists",
						"An existing account already uses email: " + ssoEmail);
			}

			User user = new User();
			user.setEmail(ssoEmail.trim().toLowerCase(Locale.ROOT));
			user.setName(resolveDisplayName(traceId, provider, request.getSsoToken(), ssoEmail));
			user.setPasswordHash(passwordEncryptionDecryption.encode(UUID.randomUUID().toString() + "-" + System.currentTimeMillis()));
			user.setStatus(ApplicationConstants.ACTIVE_USER_STATUS);
			user.setUpdatedAt(LocalDateTime.now());
			user = userRepository.save(user);

			assignRole(user, ApplicationConstants.ADMIN_ROLE);

			String accessToken = jwtTokenService.generateAccessToken(traceId, user.getEmail());
			String refreshToken = jwtTokenService.generateRefreshToken(traceId, user.getEmail());
			return createSsoAuthSuccessResponse(ResponseCode.USMG_201, accessToken, refreshToken, user);
		}
		catch (Exception ex) {
			log.error("[{}]|SSO_REGISTER|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			return buildError(ResponseCode.USMG_500, "Internal server error", traceId, method, "ServerError", ex.getMessage());
		}
	}

	@Override
	public SsoAuthResponse ssoSignUp(String traceId, SsoAuthRequest request) {
		traceId = validator.getOrGenerateRequestId(traceId);
		String method = "/sso/signup";
		log.info("[{}]|SSO_SIGNIN|STARTED|Payload: {}", traceId, request);

		try {
			if (isInvalidRequest(request, traceId)) {
				return buildError(ResponseCode.USMG_400, "Invalid request payload", traceId, method, "BadRequest",
						"Missing required fields or null values");
			}

			InvitationToken tokenEntity = validateInvitationToken(traceId, request.getInvitationToken());
			if (tokenEntity == null) {
				return buildError(ResponseCode.USMG_400, "Invalid or expired invitation token", traceId, method, "InvalidToken",
						"Token not found, expired, or already used");
			}

			SsoProvider provider = validateSsoProvider(traceId, request.getProviderId());
			if (provider == null) {
				return buildError(ResponseCode.USMG_400, "Invalid or disabled SSO provider", traceId, method, "InvalidProvider",
						"SSO provider not found or disabled");
			}

			// TODO: correction after

			String ssoEmail = validateSsoTokenAndExtractEmail(traceId, provider, request.getSsoToken());
			if (ssoEmail == null) {
				return buildError(ResponseCode.USMG_401, provider.getName() + " SSO token validation failed", traceId, method, "InvalidToken",
						"Token validation failed or email not verified");
			}

			if (!tokenEntity.getEmail().equalsIgnoreCase(ssoEmail)) {
				return buildError(ResponseCode.USMG_404, "Email mismatch. Please use invited email", traceId, method, "EmailMismatch",
						"InvitedEmail: " + request.getEmail() + ", SsoEmail: " + ssoEmail);
			}

			User user = userRepository.findByEmail(ssoEmail);
			if (user == null) {
				log.warn("[{}]|SSO_SIGNIN|USER_NOT_FOUND|Email: {}", traceId, ssoEmail);
				return buildError(ResponseCode.USMG_404, "User not found in system", traceId, method, "UserNotFound",
						"No user exists with email: " + ssoEmail);
			}

			String rawPassword = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
			log.debug("[{}]|SSO_SIGNIN|GENERATED_PASSWORD|Raw: {}", traceId, rawPassword);

			String encryptedPassword = passwordEncryptionDecryption.encode(rawPassword);
			user.setPasswordHash(encryptedPassword);
			user.setStatus(ApplicationConstants.ACTIVE_USER_STATUS);
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);
			log.info("[{}]|SSO_SIGNIN|USER_UPDATED|Email: {}", traceId, user.getEmail());

			String accessToken = jwtTokenService.generateAccessToken(traceId, user.getEmail());
			String refreshToken = jwtTokenService.generateRefreshToken(traceId, user.getEmail());
			log.info("[{}]|SSO_SIGNIN|TOKENS_GENERATED", traceId);

			tokenEntity.setUsed(true);
			invitationTokenRepository.save(tokenEntity);
			log.info("[{}]|SSO_SIGNIN|TOKEN_USED|{}", traceId, request.getInvitationToken());

			if (!setRole(traceId, user)) {
				log.warn("[{}]|SSO_SIGNIN|FAILD|Unable to assign the role to the user.", traceId);
				return buildError(ResponseCode.USMG_500, "Internal server error.", traceId, method, "Server Error",
						"Unable to assign the role to the user.");
			}
			return createSsoAuthSuccessResponse(ResponseCode.USMG_201, accessToken, refreshToken, user);
		}
		catch (Exception ex) {
			log.error("[{}]|SSO_SIGNIN|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			return buildError(ResponseCode.USMG_500, "Internal server error", traceId, method, "ServerError", ex.getMessage());
		}
	}

	public List<String> getUserRoles(Long userId) {
		return userRepository.findRoleNamesByUserId(userId);
	}

	private boolean setRole(String traceId, User user) {
		try {
			Optional<Role> matchedRole = roleRepository.findAll()
					.stream()
					.filter(role -> ApplicationConstants.USER_ROLE.equalsIgnoreCase(role.getName()))
					.findFirst();

			if (matchedRole.isPresent()) {
				Role role = matchedRole.get();

				UserRole userRole = new UserRole();
				userRole.setUserId(user.getId());
				userRole.setRoleId(role.getId());
				userRole.setAssignedBy(user.getId()); 
				userRole.setAssignedAt(LocalDateTime.now());

				userRoleRepository.save(userRole);

				log.info("[{}]|ROLE_ASSIGNMENT|SUCCESS|User {} assigned to role {}", traceId, user.getName(), role.getName());
				return true;
			}
			else {
				log.warn("[{}]|ROLE_ASSIGNMENT|FAILED|No matching role found for name: {}", traceId, ApplicationConstants.USER_ROLE);
				return false;
			}
		}
		catch (Exception ex) {
			log.error("[{}]|ROLE_ASSIGNMENT|EXCEPTION|{}", traceId, ex.getMessage(), ex);

		}

		return false;
	}

	private void assignRole(User user, String roleName) {
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

		UserRole existingRole = userRoleRepository.findFirstByUserIdAndRoleId(user.getId(), role.getId());
		if (existingRole != null) {
			return;
		}

		UserRole userRole = new UserRole();
		userRole.setUserId(user.getId());
		userRole.setRoleId(role.getId());
		userRole.setAssignedAt(LocalDateTime.now());
		userRoleRepository.save(userRole);
	}

	private boolean hasRole(Long userId, String roleName) {
		return userRoleRepository.findRoleNamesByUserId(userId).stream().anyMatch(roleName::equalsIgnoreCase);
	}

	private boolean isInvalidRequest(SsoAuthRequest request, String traceId) {
		try {
			boolean invalid = request == null || request.getProviderId() == null
					|| validator.isNullOrEmpty(traceId, request.getInvitationToken(), "invitationToken")
					|| validator.isNullOrEmpty(traceId, request.getSsoToken(), "ssoToken");

			if (invalid) {
				log.warn("[{}]|SSO_SIGNIN|INVALID_REQUEST|{}", traceId, request);
			}
			return invalid;
		}
		catch (Exception e) {
			log.error("[{}]|SSO_SIGNIN|REQUEST_VALIDATION_EXCEPTION|{}", traceId, e.getMessage(), e);
			return true;
		}
	}

	private boolean isInvalidSigiInRequest(SsoAuthRequest request, String traceId) {
		try {
			boolean invalid = request == null || request.getProviderId() == null
					|| validator.isNullOrEmpty(traceId, request.getSsoToken(), "ssoToken");

			if (invalid) {
				log.warn("[{}]|SSO_SIGNIN|INVALID_REQUEST|{}", traceId, request);
			}
			return invalid;
		}
		catch (Exception e) {
			log.error("[{}]|SSO_SIGNIN|REQUEST_VALIDATION_EXCEPTION|{}", traceId, e.getMessage(), e);
			return true;
		}
	}

	private InvitationToken validateInvitationToken(String traceId, String token) {
		try {
			InvitationToken entity = invitationTokenRepository.findByToken(token);
			if (entity == null || entity.getExpiresAt().isBefore(LocalDateTime.now()) || Boolean.TRUE.equals(entity.getUsed())) {
				log.warn("[{}]|SSO_SIGNIN|INVALID_INVITATION_TOKEN|{}", traceId, token);
				return null;
			}
			log.info("[{}]|SSO_SIGNIN|TOKEN_VALIDATED", traceId);
			return entity;
		}
		catch (Exception e) {
			log.error("[{}]|SSO_SIGNIN|TOKEN_VALIDATION_ERROR|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	private SsoProvider validateSsoProvider(String traceId, Long providerId) {
		try {
			Optional<SsoProvider> providerOpt = ssoProviderRepository.findById(providerId);
			if (providerOpt.isEmpty() || Boolean.FALSE.equals(providerOpt.get().getEnabled())) {
				log.warn("[{}]|SSO_SIGNIN|INVALID_SSO_PROVIDER|ID: {}", traceId, providerId);
				return null;
			}
			log.info("[{}]|SSO_SIGNIN|SSO_PROVIDER_VALID|{}", traceId, providerOpt.get().getName());
			return providerOpt.get();
		}
		catch (Exception e) {
			log.error("[{}]|SSO_SIGNIN|PROVIDER_VALIDATION_EXCEPTION|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	private String validateSsoTokenAndExtractEmail(String traceId, SsoProvider provider, String token) {
		try {
			if (ApplicationConstants.GOOGLE_SSO_PROVIDER.equalsIgnoreCase(provider.getName())) {
				GoogleUserDto googleUser = ssoTokenValidatorService.validateGoogleSsoToken(traceId, token);
				if (googleUser != null && googleUser.isEmailVerified()) {
					log.info("[{}]|SSO_SIGNIN|GOOGLE_EMAIL_VERIFIED|{}", traceId, googleUser.getEmail());
					return googleUser.getEmail();
				}
			}
			else if (ApplicationConstants.MICROSOFT_SSO_PROVIDER.equalsIgnoreCase(provider.getName())) {
				MicrosoftUserDto msUser = ssoTokenValidatorService.validateMicrosoftSsoToken(traceId, token);
				if (msUser != null && msUser.isEmailVerified()) {
					log.info("[{}]|SSO_SIGNIN|MICROSOFT_EMAIL_VERIFIED|{}", traceId, msUser.getEmail());
					return msUser.getEmail();
				}
			}
			log.warn("[{}]|SSO_SIGNIN|SSO_TOKEN_INVALID_OR_EMAIL_NOT_VERIFIED", traceId);
			return null;
		}
		catch (Exception e) {
			log.error("[{}]|SSO_SIGNIN|SSO_TOKEN_VALIDATION_EXCEPTION|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	private SsoAuthResponse buildError(String code, String message, String traceId, String path, String errorType, String devMessage) {
		ErrorObj errorObj = new ErrorObj();
		errorObj.setCode(code);
		errorObj.setMessage(message);
		errorObj.setPath(path);
		errorObj.setError(errorType);

		List<ErrorObj> errorList = new ArrayList<>();
		errorList.add(errorObj);

		SsoAuthResponse response = new SsoAuthResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(errorList);

		log.warn("[{}]|SSO_SIGNIN|ERROR_RESPONSE|Code: {}|Message: {}|Path: {}|ErrorType: {}|DevMsg: {}", traceId, code, message, path, errorType,
				devMessage);
		return response;
	}

	public SsoAuthResponse createSsoAuthSuccessResponse(String statusCode, String accessToken, String refreshToken, User user) {
		AuthResponse authResponse = new AuthResponse();
		authResponse.setAccessToken(accessToken);
		authResponse.setRefreshToken(refreshToken);
		Organization organization = user.getOrganization();
		authResponse.setOrganizationId(organization != null ? organization.getId() : null);
		authResponse.setRole(getUserRoles(user.getId()));

		SsoAuthResponse response = new SsoAuthResponse();
		response.setStatus(true);
		response.setCode(statusCode);
		response.setMessage("User authenticated successfully via SSO.");
		response.setTimestamp(new Date());
		response.setAuthResponse(authResponse);

		log.info("[{}]|SSO_SIGNIN|RESPONSE_SUCCESS", accessToken);
		return response;
	}

	private String resolveDisplayName(String traceId, SsoProvider provider, String token, String email) {
		try {
			if (ApplicationConstants.GOOGLE_SSO_PROVIDER.equalsIgnoreCase(provider.getName())) {
				GoogleUserDto googleUser = ssoTokenValidatorService.validateGoogleSsoToken(traceId, token);
				if (googleUser != null && googleUser.getName() != null && !googleUser.getName().isBlank()) {
					return googleUser.getName();
				}
			}
			else if (ApplicationConstants.MICROSOFT_SSO_PROVIDER.equalsIgnoreCase(provider.getName())) {
				MicrosoftUserDto microsoftUser = ssoTokenValidatorService.validateMicrosoftSsoToken(traceId, token);
				if (microsoftUser != null && microsoftUser.getName() != null && !microsoftUser.getName().isBlank()) {
					return microsoftUser.getName();
				}
			}
		}
		catch (Exception ex) {
			log.warn("[{}]|SSO_REGISTER|DISPLAY_NAME_FALLBACK|{}", traceId, ex.getMessage());
		}
		return email;
	}

	@Override
	public SsoAuthResponse ssoSignIn(String traceId, SsoAuthRequest request) {
		traceId = validator.getOrGenerateRequestId(traceId);
		String method = "/sso/signin";
		log.info("[{}]|SSO_SIGNUP|STARTED|Payload: {}", traceId, request);

		try {
			if (isInvalidSigiInRequest(request, traceId)) {
				return buildError(ResponseCode.USMG_400, "Invalid request payload", traceId, method, "BadRequest",
						"Missing required fields or null values");
			}

			SsoProvider provider = validateSsoProvider(traceId, request.getProviderId());
			if (provider == null) {
				return buildError(ResponseCode.USMG_400, "Invalid or disabled SSO provider", traceId, method, "InvalidProvider",
						"SSO provider not found or disabled");
			}

			String ssoEmail = validateSsoTokenAndExtractEmail(traceId, provider, request.getSsoToken());
			if (ssoEmail == null) {
				return buildError(ResponseCode.USMG_401, provider.getName() + " SSO token validation failed", traceId, method, "InvalidToken",
						"Token validation failed or email not verified");
			}

			User existingUser = userRepository.findByEmail(ssoEmail);
			if (existingUser == null || !existingUser.getStatus().equals(ApplicationConstants.ACTIVE_USER_STATUS)) {
				return buildError(ResponseCode.USMG_404, "User not available in our system.", traceId, method, "No user",
						"User not exist or not active useer.");
			}

			log.info("[{}]|SSO_SIGNUP|TOKEN_USED|{}", traceId, request.getInvitationToken());

			String accessToken = jwtTokenService.generateAccessToken(traceId, existingUser.getEmail());
			String refreshToken = jwtTokenService.generateRefreshToken(traceId, existingUser.getEmail());

			log.info("[{}]|SSO_SIGNUP|TOKENS_GENERATED", traceId);
			return createSsoAuthSuccessResponse(ResponseCode.USMG_201, accessToken, refreshToken, existingUser);

		}
		catch (Exception ex) {
			log.error("[{}]|SSO_SIGNUP|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			return buildError(ResponseCode.USMG_500, "Internal server error", traceId, method, "ServerError", ex.getMessage());
		}
	}

}
