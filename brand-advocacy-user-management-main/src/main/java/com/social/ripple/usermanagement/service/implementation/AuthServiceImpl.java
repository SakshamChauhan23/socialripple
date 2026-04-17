
/**
 * Filename: AuthServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.InvitationToken;
import com.social.ripple.usermanagement.dao.model.Organization;
import com.social.ripple.usermanagement.dao.model.SsoProvider;
import com.social.ripple.usermanagement.dao.model.Role;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserRole;
import com.social.ripple.usermanagement.dao.repository.InvitationTokenRepository;
import com.social.ripple.usermanagement.dao.repository.OrganizationRepository;
import com.social.ripple.usermanagement.dao.repository.RoleRepository;
import com.social.ripple.usermanagement.dao.repository.SsoProviderRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.UserRoleRepository;
import com.social.ripple.usermanagement.dto.request.LoginRequest;
import com.social.ripple.usermanagement.dto.request.OrganizationOnboardingRequest;
import com.social.ripple.usermanagement.dto.request.RefreshTokenRequest;
import com.social.ripple.usermanagement.dto.request.SignupRequest;
import com.social.ripple.usermanagement.dto.response.AuthResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.LoginResponse;
import com.social.ripple.usermanagement.dto.response.RecaptchaResponse;
import com.social.ripple.usermanagement.dto.response.SignupResponse;
import com.social.ripple.usermanagement.dto.response.SsoProviderDto;
import com.social.ripple.usermanagement.dto.response.SsoProviderResponse;
import com.social.ripple.usermanagement.dto.response.UserDataDto;
import com.social.ripple.usermanagement.dto.response.UserProfileDTO;
import com.social.ripple.usermanagement.security.config.PasswordEncryptionDecryption;
import com.social.ripple.usermanagement.security.jwt.JwtUtils;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IAuthService;
import com.social.ripple.usermanagement.service.RecaptchaService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {

	private final SsoProviderRepository ssoProviderRepository;
	private final JwtUtils jwtUtils;
	private final AuthenticationManager authenticationManager;
	private final RecaptchaService recaptchaService;
	private final AppCommonValidator appCommonValidator;
	private final UserRepository userRepository;
	private final InvitationTokenRepository invitationTokenRepository;
	private final PasswordEncryptionDecryption passwordEncryption;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final OrganizationRepository organizationRepository;
	@Value("${app.security.captcha.enabled:true}")
	private boolean captchaEnabled;
	@Value("${app.security.recaptcha.min-score:0.1}")
	private float recaptchaMinScore;

	public AuthServiceImpl(SsoProviderRepository ssoProviderRepository, JwtUtils jwtUtils, AuthenticationManager authenticationManager,
			RecaptchaService recaptchaService, AppCommonValidator appCommonValidator, UserRepository userRepository,
			InvitationTokenRepository invitationTokenRepository, PasswordEncryptionDecryption passwordEncryption,
			RoleRepository roleRepository, UserRoleRepository userRoleRepository, OrganizationRepository organizationRepository) {
		this.ssoProviderRepository = ssoProviderRepository;
		this.jwtUtils = jwtUtils;
		this.authenticationManager = authenticationManager;
		this.recaptchaService = recaptchaService;
		this.appCommonValidator = appCommonValidator;
		this.userRepository = userRepository;
		this.invitationTokenRepository = invitationTokenRepository;
		this.passwordEncryption = passwordEncryption;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.organizationRepository = organizationRepository;
	}

	@Override
	public LoginResponse login(String xTraceId, String xCorrelationId, LoginRequest loginRequest) {
		log.info("[{}]|auth|login|start|username:{}", xTraceId, loginRequest.getUserName());

		LoginResponse response = new LoginResponse();
		response.setTimestamp(new Date());

		if (!validateLoginRequest(xTraceId, loginRequest, response)) {
			return response;
		}
		try {
			if (!verifyCaptcha(xTraceId, loginRequest.getCaptchaToken(), response)) {
				return response;
			}

			Authentication authentication = authenticateUser(xTraceId, loginRequest, response);

			if (authentication == null) {
				log.warn("[{}]|auth|login|failure|null_authentication", xTraceId);
				return buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid credentials", "Incorrect username or password", null,
						response);
			}
			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			if (!checkAccountLock(xTraceId, userDetails, response)) {
				return response;
			}
			return buildSuccessResponse(xTraceId, userDetails, response);
		}
		catch (BadCredentialsException ex) {
			log.warn("[{}]|auth|login|failure|bad_credentials|{}", xTraceId, ex.getMessage());
			return buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid credentials(Bad credentials)", "Incorrect username or password",
					null, response);
		}
		catch (LockedException ex) {
			log.warn("[{}]|auth|login|failure|account_locked|{}", xTraceId, ex.getMessage());
			return buildErrorResponse(ResponseCode.USMG_423, "Account is temporarily locked. Try again later.", null, null, response);
		}
		catch (UsernameNotFoundException ex) {
			log.warn("[{}]|auth|login|failure|user_not_found|{}", xTraceId, ex.getMessage());
			return buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid credentials", ex.getMessage(), null, response);
		}
		catch (Exception ex) {
			log.error("[{}]|auth|login|error|{}", xTraceId, ex.getMessage(), ex);
			return buildErrorResponse(ResponseCode.USMG_500, "Internal Server Error", ex.getMessage(), null, response);
		}
	}

	private boolean validateLoginRequest(String xTraceId, LoginRequest loginRequest, LoginResponse response) {
		if (appCommonValidator.isNullOrEmpty(xTraceId, loginRequest.getUserName(), "userName")
				|| appCommonValidator.isNullOrEmpty(xTraceId, loginRequest.getPassword(), "password")) {
			log.warn("[{}]|auth|login|failure|missing_credentials", xTraceId);
			buildErrorResponse(ResponseCode.USMG_400, "Bad Request", "Username or password is required.",
					new ErrorObj("username", ResponseCode.USMG_400, "NotNull.LoginRequest.username", "Username or password is required."), response);
			return false;
		}
		if (captchaEnabled && (loginRequest.getCaptchaToken() == null || loginRequest.getCaptchaToken().isEmpty())) {
			log.warn("[{}]|auth|login|failure|missing_captcha_token", xTraceId);
			buildErrorResponse(ResponseCode.USMG_400, "Captcha Token is required", null,
					new ErrorObj("captchaToken", ResponseCode.USMG_400, "NotNull.LoginRequest.captchaToken", "Captcha token must not be empty."),
					response);
			return false;
		}

		return true;
	}

	private boolean verifyCaptcha(String xTraceId, String captchaToken, LoginResponse response) {
		if (!captchaEnabled) {
			log.info("[{}]|auth|login|captcha_skipped|reason:captcha_disabled", xTraceId);
			return true;
		}
		try {
			RecaptchaResponse captchaResponse = recaptchaService.verifyToken(xTraceId, captchaToken);

			if (isCaptchaRejected(captchaResponse)) {
				log.warn("[{}]|auth|login|failure|captcha_verification_failed|score:{}|threshold:{}|hostname:{}|errorCodes:{}", xTraceId,
						captchaResponse != null ? captchaResponse.getScore() : "null", recaptchaMinScore,
						captchaResponse != null ? captchaResponse.getHostname() : "null",
						captchaResponse != null ? captchaResponse.getErrorCodes() : "null");
				buildErrorResponse(ResponseCode.USMG_403, "Captcha verification failed", buildCaptchaFailureDevMessage(captchaResponse), null, response);
				return false;
			}
			return true;

		}
		catch (Exception ex) {
			log.error("[{}]|auth|login|captcha_verification_error|{}", xTraceId, ex.getMessage(), ex);
			buildErrorResponse(ResponseCode.USMG_500, "Captcha validation error", ex.getMessage(), null, response);
			return false;
		}
	}

	private Authentication authenticateUser(String xTraceId, LoginRequest loginRequest, LoginResponse response) {
		try {
			return authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword()));
		}
		catch (BadCredentialsException ex) {
			log.warn("[{}]|auth|login|failure|bad_credentials|{}", xTraceId, ex.getMessage());
			buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid credentials", "Incorrect username or password", null, response);
		}
		catch (UsernameNotFoundException ex) {
			log.warn("[{}]|auth|login|failure|user_not_found|{}", xTraceId, ex.getMessage());
			buildErrorResponse(ResponseCode.USMG_401, "Unauthorized - Invalid credentials", ex.getMessage(), null, response);
		}
		catch (Exception ex) {
			log.error("[{}]|auth|login|authentication_error|{}", xTraceId, ex.getMessage(), ex);
			buildErrorResponse(ResponseCode.USMG_500, "Authentication Error", ex.getMessage(), null, response);
		}
		return null;
	}

	private boolean checkAccountLock(String xTraceId, UserDetailsImpl userDetails, LoginResponse response) {
		if (!userDetails.isAccountNonLocked()) {
			log.warn("[{}]|auth|login|failure|account_locked|userId:{}", xTraceId, userDetails.getUserId());
			buildErrorResponse(ResponseCode.USMG_423, "Account is temporarily locked. Try again later.", null, null, response);
			return false;
		}
		return true;
	}

	private LoginResponse buildSuccessResponse(String xTraceId, UserDetailsImpl userDetails, LoginResponse response) {
		try {
			String accessToken = jwtUtils.generateAccessToken(xTraceId, userDetails.getUsername());
			String refreshToken = jwtUtils.generateRefreshToken(xTraceId, userDetails.getUsername());

			UserProfileDTO userProfileDTO = new UserProfileDTO();
			userProfileDTO.setUserId(userDetails.getUserId());
			userProfileDTO.setUsername(userDetails.getUsername());
			userProfileDTO.setEmail(userDetails.getUsername());

			UserDataDto userData = new UserDataDto();
			userData.setAccessToken(accessToken);
			userData.setRefreshToken(refreshToken);
			Long orgId = getOrganizationId(xTraceId, userDetails.getUsername());
			userData.setOrganizationId(orgId);
			List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

			userData.setRole(roles);

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Login Successful.");
			response.setDevMessage(null);
			response.setErrors(null);
			response.setUserData(userData);

			log.info("[{}]|auth|login|success|userId:{}|username:{}", xTraceId, userDetails.getUserId(), userDetails.getUsername());

		}
		catch (Exception ex) {
			log.error("[{}]|auth|login|token_generation_failed|{}", xTraceId, ex.getMessage(), ex);
			return buildErrorResponse(ResponseCode.USMG_500, "Token generation failed", ex.getMessage(), null, response);
		}

		return response;
	}

	private Long getOrganizationId(String xTraceId, String userName) {
		log.info("[{}]|auth|getOrganizationId|start|userName:{}", xTraceId, userName);

		if (userName == null || userName.isBlank()) {
			log.warn("[{}]|auth|getOrganizationId|invalid_userName|null_or_blank", xTraceId);
			return null;
		}
		if (userRepository == null) {
			log.error("[{}]|auth|getOrganizationId|repo_null", xTraceId);
			return null;
		}

		try {
			User user = userRepository.findByEmail(userName);
			if (user == null) {
				log.warn("[{}]|auth|getOrganizationId|user_not_found|email:{}", xTraceId, userName);
				return null;
			}

			Organization org = user.getOrganization();
			if (org == null) {
				log.warn("[{}]|auth|getOrganizationId|org_null|userId:{}", xTraceId, user.getId());
				return null;
			}

			Long orgId = org.getId();
			log.info("[{}]|auth|getOrganizationId|success|userId:{}|orgId:{}", xTraceId, user.getId(), orgId);
			return orgId;

		}
		catch (Exception ex) {
			log.error("[{}]|auth|getOrganizationId|error|{}", xTraceId, ex.getMessage(), ex);
			return null;
		}
	}

	@Override
	public SsoProviderResponse ssoProviders(String xTraceId, String captchaToken) {
		log.info("[{}]|auth|ssoProviders|start", xTraceId);

		SsoProviderResponse response = new SsoProviderResponse();
		response.setTimestamp(new Date());

		if (!verifyCaptcha(xTraceId, captchaToken, response)) {
			return response; // Captcha failed
		}

		try {
			List<SsoProvider> providerEntities = ssoProviderRepository.findAll();
			List<SsoProviderDto> providerList = new ArrayList<>();

			for (SsoProvider provider : providerEntities) {
				if (provider != null && provider.getId() != null && provider.getName() != null) {
					providerList.add(new SsoProviderDto(provider.getId(), provider.getName()));
				}
			}

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("SSO providers fetched successfully");
			response.setProviderData(providerList);

			log.info("[{}]|auth|ssoProviders|success|providerCount:{}", xTraceId, providerList.size());
		}
		catch (Exception e) {
			log.error("[{}]|auth|ssoProviders|error|{}", xTraceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setProviderData(Collections.emptyList());
		}

		return response;
	}

	@Override
	public ResponseEntity<SignupResponse> register(SignupRequest request, String traceId) {
		log.info("[{}]|USMG|Register|Start|email:{}", traceId, request != null ? request.getEmail() : "null");
		SignupResponse response = new SignupResponse();
		try {
			ResponseEntity<SignupResponse> validationError = validateDirectRegistrationRequest(request, traceId, response);
			if (validationError != null) {
				return validationError;
			}

			// Check for duplicate email before creating user
			if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
				log.warn("[{}]|USMG|Register|DuplicateEmail|{}", traceId, request.getEmail());
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_409,
								"User already exists", "An account with this email already exists. Please sign in."));
			}

			User user = new User();
			user.setName((request.getFirstName().trim() + " " + request.getLastName().trim()).trim());
			user.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
			user.setPhone(request.getMobileNumber());
			user.setPasswordHash(passwordEncryption.encode(request.getPassword()));
			user.setStatus(ApplicationConstants.USER_ACTIVE_STATUS);
			user.setUpdatedAt(LocalDateTime.now());
			user = userRepository.save(user);
			assignRoleToUser(user, ApplicationConstants.ADMIN_ROLE);

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_201);
			response.setMessage("Signup successful.");
			response.setDevMessage("User created successfully. Sign in to continue organization setup.");
			response.setUserId(String.valueOf(user.getId()));
			response.setTimestamp(new Date());
			return ResponseEntity.status(201).body(response);
		}
		catch (IllegalStateException ex) {
			log.warn("[{}]|USMG|Register|Conflict|{}", traceId, ex.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_409, "User already exists", ex.getMessage()));
		}
		catch (org.springframework.dao.DataIntegrityViolationException ex) {
			log.warn("[{}]|USMG|Register|DuplicateKey|{}", traceId, ex.getMessage());
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_409,
							"User already exists", "An account with this email already exists. Please sign in."));
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|Register|Exception|{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.internalServerError()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_500, "Internal server error", ex.getMessage()));
		}
	}

	private boolean verifyCaptcha(String xTraceId, String captchaToken, SsoProviderResponse response) {
		if (!captchaEnabled) {
			log.info("[{}]|auth|ssoProviders|captcha_skipped|reason:captcha_disabled", xTraceId);
			return true;
		}
		try {
			RecaptchaResponse captchaResponse = recaptchaService.verifyToken(xTraceId, captchaToken);

			if (isCaptchaRejected(captchaResponse)) {
				log.warn("[{}]|auth|ssoProviders|captcha_verification_failed|score:{}|threshold:{}|hostname:{}|errorCodes:{}", xTraceId,
						captchaResponse != null ? captchaResponse.getScore() : "null", recaptchaMinScore,
						captchaResponse != null ? captchaResponse.getHostname() : "null",
						captchaResponse != null ? captchaResponse.getErrorCodes() : "null");

				response.setStatus(false);
				response.setCode(ResponseCode.USMG_403);
				response.setMessage("Captcha verification failed");
				response.setDevMessage(buildCaptchaFailureDevMessage(captchaResponse));
				response.setProviderData(Collections.emptyList());
				return false;
			}
			return true;

		}
		catch (Exception ex) {
			log.error("[{}]|auth|ssoProviders|captcha_verification_error|{}", xTraceId, ex.getMessage(), ex);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Captcha validation error");
			response.setDevMessage(ex.getMessage());
			response.setProviderData(Collections.emptyList());
			return false;
		}
	}

	private boolean isCaptchaRejected(RecaptchaResponse captchaResponse) {
		return captchaResponse == null || !captchaResponse.isSuccess() || captchaResponse.getScore() < recaptchaMinScore;
	}

	private String buildCaptchaFailureDevMessage(RecaptchaResponse captchaResponse) {
		if (captchaResponse == null) {
			return "reCAPTCHA verification returned no response.";
		}

		if (!captchaResponse.isSuccess()) {
			String errorCodes = captchaResponse.getErrorCodes() == null || captchaResponse.getErrorCodes().isEmpty()
					? "verification_failed"
					: String.join(", ", captchaResponse.getErrorCodes());
			if (captchaResponse.getHostname() != null && !captchaResponse.getHostname().isBlank()) {
				return String.format("reCAPTCHA verification failed: %s (hostname: %s)", errorCodes, captchaResponse.getHostname());
			}
			return String.format("reCAPTCHA verification failed: %s", errorCodes);
		}

		return String.format(Locale.US, "Low reCAPTCHA score: %.2f (minimum: %.2f)%s", captchaResponse.getScore(), recaptchaMinScore,
				captchaResponse.getHostname() != null && !captchaResponse.getHostname().isBlank()
						? String.format(" (hostname: %s)", captchaResponse.getHostname())
						: "");
	}

	private LoginResponse buildErrorResponse(String code, String message, String devMessage, ErrorObj error, LoginResponse response) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setErrors(error != null ? List.of(error) : null);
		response.setUserData(null);
		response.setUserProfile(null);
		return response;
	}

	@Override
	public ResponseEntity<?> getInviteInfo(String token, String traceId) {
		log.info("[{}]|USMG|InviteInfo|Start|token:{}", traceId, token);
		if (token == null || token.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("status", false, "message", "Token is required"));
		}
		Optional<InvitationToken> tokenOpt = invitationTokenRepository.findByTokenAndUsedFalse(token);
		if (tokenOpt.isEmpty()) {
			return ResponseEntity.status(404).body(Map.of("status", false, "message", "Invalid or already used invitation token"));
		}
		InvitationToken inviteToken = tokenOpt.get();
		if (inviteToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			return ResponseEntity.status(410).body(Map.of("status", false, "message", "Invitation has expired"));
		}
		User user = userRepository.findByEmail(inviteToken.getEmail());
		if (user == null) {
			return ResponseEntity.status(404).body(Map.of("status", false, "message", "Invited user not found"));
		}
		String fullName = user.getName() != null ? user.getName() : "";
		String[] nameParts = fullName.trim().split("\\s+", 2);
		String firstName = nameParts.length > 0 ? nameParts[0] : "";
		String lastName = nameParts.length > 1 ? nameParts[1] : "";
		String orgName = (user.getOrganization() != null) ? user.getOrganization().getName() : "";

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", true);
		result.put("firstName", firstName);
		result.put("lastName", lastName);
		result.put("email", inviteToken.getEmail());
		result.put("phone", user.getPhone() != null ? user.getPhone() : "");
		result.put("organizationName", orgName);
		return ResponseEntity.ok(result);
	}

	@Override
	public ResponseEntity<SignupResponse> signupViaInvitation(SignupRequest request, String tenantId, String traceId) {
		log.info("[{}]|USMG|Signup|Start|tenantId:{}|email:{}", traceId, tenantId, request != null ? request.getEmail() : "null");

		SignupResponse response = new SignupResponse();

		try {
			// If tenantId is missing (new user has no session), derive it from the invitation token
			if ((tenantId == null || tenantId.isBlank()) && request != null && request.getInviteToken() != null) {
				Optional<InvitationToken> tokenLookup = invitationTokenRepository.findByTokenAndUsedFalse(request.getInviteToken());
				if (tokenLookup.isPresent() && tokenLookup.get().getOrganization() != null) {
					tenantId = String.valueOf(tokenLookup.get().getOrganization().getId());
					log.info("[{}]|USMG|Signup|TenantId derived from invite token: {}", traceId, tenantId);
				}
			}

			ResponseEntity<SignupResponse> validationError = validateRequest(request, tenantId, traceId, response);
			if (validationError != null)
				return validationError;

			Optional<InvitationToken> tokenOpt = invitationTokenRepository.findByTokenAndUsedFalse(request.getInviteToken());
			if (tokenOpt.isEmpty()) {
				log.warn("[{}]|Signup|TokenValidation|Failed|Invalid or already used token", traceId);
				return ResponseEntity.status(401)
						.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_401, "Invalid or used token",
								"Token not found or already used"));
			}

			InvitationToken token = tokenOpt.get();

			if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
				log.warn("[{}]|Signup|TokenValidation|Failed|Expired token", traceId);
				return ResponseEntity.status(401)
						.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_401, "Token expired",
								"Expired at " + token.getExpiresAt()));
			}

			if (!token.getEmail().equalsIgnoreCase(request.getEmail())) {
				log.warn("[{}]|Signup|TokenValidation|Failed|Email mismatch|Expected:{}|Provided:{}", traceId, token.getEmail(), request.getEmail());
				return ResponseEntity.status(409)
						.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_409, "Email mismatch",
								"Email does not match invitation"));
			}

			User existingUser = userRepository.findByEmail(request.getEmail());
			if (existingUser == null) {
				log.warn("[{}]|Signup|No invited user found for email: {}", traceId, request.getEmail());
				return ResponseEntity.status(404)
						.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_404, "User not found",
								"Invited user with this email does not exist"));
			}

			existingUser.setName(request.getFirstName() + " " + request.getLastName());
			existingUser.setPhone(request.getMobileNumber());
			existingUser.setPasswordHash(passwordEncryption.encode(request.getPassword()));
			existingUser.setStatus(ApplicationConstants.USER_ACTIVE_STATUS);
			existingUser.setUpdatedAt(LocalDateTime.now());
			userRepository.save(existingUser);

			token.setUsed(true);
			invitationTokenRepository.save(token);

			response.setUserId(String.valueOf(existingUser.getId()));
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("User registered successfully.");
			response.setDevMessage("Signup completed and user activated.");
			response.setTimestamp(new Date());

			log.info("[{}]|Signup|Success|userId:{}", traceId, existingUser.getId());
			return ResponseEntity.ok(response);

		}
		catch (Exception ex) {
			log.error("[{}]|Signup|Exception|{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.internalServerError()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_500, "Internal server error", ex.getMessage()));
		}
	}

	@Override
	public AuthResponse completeOrganizationOnboarding(String traceId, UserDetailsImpl userDetails, OrganizationOnboardingRequest request) {
		AuthResponse response = new AuthResponse();
		response.setTimestamp(new Date());

		if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
			return buildErrorResponse(response, ResponseCode.USMG_401, "Unauthorized", "Authenticated user not found.");
		}
		if (request == null || request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty()) {
			return buildErrorResponse(response, ResponseCode.USMG_400, "Organization name is required.");
		}

		try {
			User user = userRepository.findByEmail(userDetails.getUsername());
			if (user == null) {
				return buildErrorResponse(response, ResponseCode.USMG_404, "User not found.");
			}
			if (user.getOrganization() != null && user.getOrganization().getId() != null) {
				return buildErrorResponse(response, ResponseCode.USMG_409, "Organization already configured for this user.");
			}

			Organization organization = new Organization();
			organization.setName(request.getOrganizationName().trim());
			organization = organizationRepository.save(organization);

			user.setOrganization(organization);
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Organization created successfully.");
			response.setOrganizationId(organization.getId());
			response.setRole(userRoleRepository.findRoleNamesByUserId(user.getId()));
			response.setAccessToken(jwtUtils.generateAccessToken(traceId, user.getEmail()));
			response.setRefreshToken(jwtUtils.generateRefreshToken(traceId, user.getEmail()));
			return response;
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|OrganizationOnboarding|Exception|{}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(response, ResponseCode.USMG_500, "Internal server error.");
		}
	}

	private ResponseEntity<SignupResponse> validateRequest(SignupRequest request, String tenantId, String traceId, SignupResponse response) {

		if (request == null || hasEmptyRequiredFields(request, tenantId, traceId)) {
			log.warn("[{}]|Signup|Validation|Missing required fields", traceId);
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_400, "Missing required fields",
							"Email, token, password or tenantId missing"));
		}

		if (!appCommonValidator.isValidEmail(traceId, request.getEmail())) {
			log.warn("[{}]|Signup|Validation|Invalid email: {}", traceId, request.getEmail());
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_400, "Invalid email format", "Email does not match format"));
		}

		if (!request.getPassword().equals(request.getConfirmPassword())) {
			log.warn("[{}]|Signup|Validation|Password mismatch", traceId);
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_400, "Passwords do not match",
							"password != confirmPassword"));
		}

		if (!appCommonValidator.isValidPassword(traceId, request.getPassword(), "password")) {
			log.warn("[{}]|Signup|Validation|Weak password", traceId);
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_400, "Weak password",
							"Does not meet complexity requirements"));
		}

		if (!appCommonValidator.isNumeric(traceId, tenantId, "tenantId")) {
			log.warn("[{}]|Signup|Validation|Non-numeric tenantId: {}", traceId, tenantId);
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/signup", ResponseCode.USMG_400, "Invalid tenant ID format",
							"Tenant ID must be numeric"));
		}

		return null;
	}

	private ResponseEntity<SignupResponse> validateDirectRegistrationRequest(SignupRequest request, String traceId, SignupResponse response) {
		if (request == null) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Missing required fields", "Request body is required"));
		}

		if (appCommonValidator.isNullOrEmpty(traceId, request.getFirstName(), "firstName")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getLastName(), "lastName")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getEmail(), "email")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getMobileNumber(), "mobileNumber")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getPassword(), "password")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getConfirmPassword(), "confirmPassword")) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Missing required fields",
							"First name, last name, email, mobile number, password and confirm password are required"));
		}

		if (!appCommonValidator.isValidEmail(traceId, request.getEmail())) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Invalid email format", "Email does not match format"));
		}

		if (!AppCommonValidator.isValidMobileNumber(request.getMobileNumber())) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Invalid mobile number",
							"Mobile number format invalid: " + request.getMobileNumber()));
		}

		if (!request.getPassword().equals(request.getConfirmPassword())) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Passwords do not match",
							"password != confirmPassword"));
		}

		if (!appCommonValidator.isValidPassword(traceId, request.getPassword(), "password")) {
			return ResponseEntity.badRequest()
					.body(buildErrorResponse(response, "/auth/register", ResponseCode.USMG_400, "Weak password",
							"Does not meet complexity requirements"));
		}

		User existingUser = userRepository.findByEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
		if (existingUser != null) {
			throw new IllegalStateException("An account already exists for email: " + request.getEmail());
		}
		return null;
	}

	private boolean hasEmptyRequiredFields(SignupRequest request, String tenantId, String traceId) {
		return appCommonValidator.isNullOrEmpty(traceId, request.getEmail(), "email")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getInviteToken(), "inviteToken")
				|| appCommonValidator.isNullOrEmpty(traceId, tenantId, "tenantId")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getPassword(), "password")
				|| appCommonValidator.isNullOrEmpty(traceId, request.getConfirmPassword(), "confirmPassword");
	}

	private SignupResponse buildErrorResponse(SignupResponse response, String path, String code, String message, String devMessage) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(Collections.singletonList(new ErrorObj(path, code, message, devMessage)));
		return response;
	}

	private void assignRoleToUser(User user, String roleName) {
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
	@Override
	public AuthResponse refreshToken(String traceId, String tenantId, RefreshTokenRequest tokenRequest) {

		AuthResponse authResponse = new AuthResponse();
		authResponse.setTimestamp(new Date());

		log.info("[{}]|AUTH|REFRESH_TOKEN|START", traceId);

		try {
			if (tokenRequest == null || tokenRequest.getRefreshToken() == null || tokenRequest.getRefreshToken().isBlank()) {
				log.warn("[{}]|AUTH|REFRESH_TOKEN|MISSING_REFRESH_TOKEN", traceId);
				return buildErrorResponse(authResponse, ResponseCode.USMG_400, "Refresh token must be provided.");
			}

			String requestRefreshToken = tokenRequest.getRefreshToken();

			if (!jwtUtils.validateRefreshToken(traceId, requestRefreshToken)) {
				log.warn("[{}]|AUTH|REFRESH_TOKEN|JWT_INVALID_OR_EXPIRED", traceId);
				return buildErrorResponse(authResponse, ResponseCode.USMG_401, "Refresh token is invalid or expired.");
			}

			String userName = jwtUtils.getUserIdFromJwtToken(traceId, requestRefreshToken);
			if (userName == null) {
				log.warn("[{}]|AUTH|REFRESH_TOKEN|USERNAME_EXTRACTION_FAILED", traceId);
				return buildErrorResponse(authResponse, ResponseCode.USMG_401, "Unable to extract user from token.");
			}
			String newAccessToken = jwtUtils.generateAccessToken(traceId, userName);
			String newRefreshToken = jwtUtils.generateRefreshToken(traceId, userName);

			authResponse.setAccessToken(newAccessToken);
			authResponse.setRefreshToken(newRefreshToken);
			authResponse.setStatus(true);
			authResponse.setCode(ResponseCode.USMG_200);
			authResponse.setMessage("Token refreshed successfully");

			log.info("[{}]|AUTH|REFRESH_TOKEN|SUCCESS|New tokens issued for user: {}", traceId, userName);

		}
		catch (Exception ex) {
			log.error("[{}]|AUTH|REFRESH_TOKEN|ERROR|Unexpected error occurred: {}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(authResponse, ResponseCode.USMG_500, "Internal Server Error");
		}

		return authResponse;
	}

	private AuthResponse buildErrorResponse(AuthResponse response, String code, String message) {
		return buildErrorResponse(response, code, message, null);
	}

	private AuthResponse buildErrorResponse(AuthResponse response, String code, String message, String devMessage) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setErrors(null);
		response.setTimestamp(new Date());
		response.setAccessToken(null);
		response.setRefreshToken(null);

		return response;
	}
}
