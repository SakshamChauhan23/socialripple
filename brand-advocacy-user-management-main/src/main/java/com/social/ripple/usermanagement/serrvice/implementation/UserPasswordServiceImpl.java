/**
 * Filename: UserPasswordServiceImpl.java
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
package com.social.ripple.usermanagement.serrvice.implementation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.request.ResetPasswordRequestDTO;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.ResetPasswordResponseDTO;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.serrvice.IUserPasswordService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserPasswordServiceImpl implements IUserPasswordService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AppCommonValidator appCommonValidator;

    @Autowired
    public UserPasswordServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,AppCommonValidator appCommonValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appCommonValidator=appCommonValidator;
    }

    @Override
    public ResetPasswordResponseDTO resetPassword(String traceId, String correlationId, String tenantId, String languageId,
            UserDetailsImpl detailsImpl, ResetPasswordRequestDTO request) {

        log.info("[{}]|RESET_PASSWORD|START|Reset password request received", traceId);
        ResetPasswordResponseDTO response = new ResetPasswordResponseDTO();
        response.setTimestamp(new Date());

        try {

            if (!Long.valueOf(tenantId).equals(detailsImpl.getOrganization().getId())) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_401);
                response.setMessage("Unauthorized: Admin organization mismatch");
                response.setErrors(List.of(
                    new ErrorObj("/auth/reset-password", ResponseCode.USMG_401,
                            "ORG_MISMATCH", "You are not authorized to reset password for this organization.")
                ));
                log.warn("[{}]|RESET_PASSWORD|AUTH_FAILED|Tenant ID {} does not match user's organization ID {}",
                        traceId, tenantId, detailsImpl.getOrganization().getId());
                return response;
            }
            
            if (request == null) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_400);
                response.setMessage("Bad Request - Request body is missing");
                response.setErrors(List.of(
                    new ErrorObj("/auth/reset-password", ResponseCode.USMG_400,
                            "REQUEST_BODY_MISSING", "Reset password request cannot be null.")
                ));
                log.warn("[{}]|RESET_PASSWORD|VALIDATION_FAILED|Request body is null", traceId);
                return response;
            }

            List<ErrorObj> errorList = new ArrayList<>();
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank() ||
            	    request.getNewPassword() == null || request.getNewPassword().isBlank() ||
            	    request.getConfirmNewPassword() == null || request.getConfirmNewPassword().isBlank()) {

            	    response.setStatus(false);
            	    response.setCode(ResponseCode.USMG_400);
            	    response.setMessage("Bad Request - Missing or blank password fields");
            	    response.setErrors(List.of(
            	        new ErrorObj("/auth/reset-password", ResponseCode.USMG_400,
            	                "MISSING_FIELDS", "All password fields (current, new, confirm) must be provided and non-blank.")
            	    ));
            	    log.warn("[{}]|RESET_PASSWORD|VALIDATION_FAILED|One or more password fields are null/blank", traceId);
            	    return response;
            	}
   
            if (!appCommonValidator.isValidPassword(traceId, request.getNewPassword(), "newPassword") ||
                    !appCommonValidator.isValidPassword(traceId, request.getConfirmNewPassword(), "confirmNewPassword")) {

                errorList.add(new ErrorObj("/auth/reset-password", ResponseCode.USMG_400,
                        "PASSWORD_POLICY_FAILED", "Password must be 6-16 chars, include uppercase, lowercase, digit, and special character."));

                response.setStatus(false);
                response.setCode(ResponseCode.USMG_400);
                response.setMessage("Bad Request - Password policy validation failed");
                response.setErrors(errorList);
                log.warn("[{}]|RESET_PASSWORD|POLICY_FAILED|Password policy not met", traceId);
                return response;
            }
       
            if (!passwordEncoder.matches(request.getCurrentPassword(), detailsImpl.getPassword())) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_401);
                response.setMessage("Unauthorized - Current password is incorrect.");
                response.setErrors(List.of(
                    new ErrorObj("/auth/reset-password", ResponseCode.USMG_401,
                            "INVALID_CURRENT_PASSWORD", "The current password provided is incorrect.")
                ));
                log.warn("[{}]|RESET_PASSWORD|AUTH_FAILED|Invalid current password provided for user {}", traceId, detailsImpl.getUserId());
                return response;
            }


            if (passwordEncoder.matches(request.getNewPassword(), detailsImpl.getPassword())) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_400);
                response.setMessage("BAD_REQUEST");
                response.setErrors(List.of(
                        new ErrorObj("/auth/reset-password", ResponseCode.USMG_400,
                                "PASSWORD_REUSE_NOT_ALLOWED", "New password must be different from current password.")
                ));
                log.warn("[{}]|RESET_PASSWORD|ERROR|New password is same as current password", traceId);
                return response;
            }

            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_400);
                response.setMessage("BAD_REQUEST");
                response.setErrors(List.of(new ErrorObj("/auth/reset-password", ResponseCode.USMG_400,
                        "PASSWORD_MISMATCH", "New password and confirm password do not match.")));
                log.warn("[{}]|RESET_PASSWORD|ERROR|New password and confirmation do not match", traceId);
                return response;
            }


            Optional<User> userOptional = userRepository.findById(detailsImpl.getUserId());
            if (userOptional.isEmpty()) {
                response.setStatus(false);
                response.setCode(ResponseCode.USMG_401);
                response.setMessage("Unauthorized - User not found.");
                response.setErrors(List.of(
                        new ErrorObj("/auth/reset-password", ResponseCode.USMG_401,
                                "INVALID_USER", "User does not exist.")
                ));
                log.warn("[{}]|RESET_PASSWORD|ERROR|User not found with ID {}", traceId, detailsImpl.getUserId());
                return response;
            }


            User user = userOptional.get();
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Password updated successfully.");
            log.info("[{}]|RESET_PASSWORD|SUCCESS|Password reset successful", traceId);
            return response;

        } catch (Exception e) {
            response.setStatus(false);
            response.setCode("USMG_500");
            response.setMessage("Internal server error. Please try again later.");
            log.error("[{}]|RESET_PASSWORD|EXCEPTION|{}", traceId, e.getMessage(), e);
            return response;
        }
    }
}
