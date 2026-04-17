
/**
 * Filename: AuthService.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.service;

import org.springframework.http.ResponseEntity;
import com.social.ripple.usermanagement.dto.request.LoginRequest;
import com.social.ripple.usermanagement.dto.request.OrganizationOnboardingRequest;
import com.social.ripple.usermanagement.dto.request.RefreshTokenRequest;
import com.social.ripple.usermanagement.dto.response.AuthResponse;
import com.social.ripple.usermanagement.dto.response.LoginResponse;
import com.social.ripple.usermanagement.dto.response.SsoProviderResponse;
import com.social.ripple.usermanagement.dto.request.SignupRequest;
import com.social.ripple.usermanagement.dto.response.SignupResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;

public interface IAuthService {
	public LoginResponse login(String xTraceId, String xCorrelationId, LoginRequest loginrequest);

	public SsoProviderResponse ssoProviders(String xTraceId, String captchaToken);

	ResponseEntity<SignupResponse> register(SignupRequest request, String traceId);

	ResponseEntity<SignupResponse> signupViaInvitation(SignupRequest request, String tenantId, String traceId);

	ResponseEntity<?> getInviteInfo(String token, String traceId);

	AuthResponse completeOrganizationOnboarding(String traceId, UserDetailsImpl userDetails, OrganizationOnboardingRequest request);

	public AuthResponse refreshToken(String traceId, String tenantId, RefreshTokenRequest tokenRequest);
}
