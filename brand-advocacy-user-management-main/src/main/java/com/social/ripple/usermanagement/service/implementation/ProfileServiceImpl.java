/**
 * Filename: ProfileServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import com.social.ripple.usermanagement.dao.model.Team;
import com.social.ripple.usermanagement.dao.model.UserTeam;
import com.social.ripple.usermanagement.dao.repository.TeamRepository;
import com.social.ripple.usermanagement.dao.repository.UserTeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.ProfileRequestDTO;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.ProfileDataResponse;
import com.social.ripple.usermanagement.dto.response.ProfileResponseDTO;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IProfileService;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ProfileConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileServiceImpl implements IProfileService {

	private UserRepository userRepository;

	@Autowired
	private UserTeamRepository userTeamRepository;

	@Autowired
	public ProfileServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public ProfileDataResponse viewProfile(String traceId, Long userId, UserDetailsImpl userDetails) {
		log.info("[{}]|PROFILE|VIEW|Start profile fetch", traceId);
		ProfileDataResponse response = new ProfileDataResponse();
		response.setTimestamp(new Date());

		try {
			String emailToSearch;

			if (userId!=null) {

//				boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> ApplicationConstants.ADMIN_ROLE.equals(a.getAuthority()));
//
//				if (!isAdmin) {
//					log.warn("[{}]|PROFILE|VIEW|Access denied. Non-admin tried to access another user's profile. userId={}", traceId, userId);
//					return buildErrorProfileResponse(response, ResponseCode.USMG_403, "Access denied. Only admins can access other users' profiles.",
//							null);
//				}

				log.info("[{}]|PROFILE|VIEW|Admin access granted to fetch userId: {}", traceId, userId);

			}
			else {
				userId = userDetails.getUserId();
				log.info("[{}]|PROFILE|VIEW|Fetching own profile for email: {}", traceId, userId);
			}

//			if (userId == null) {
//				log.warn("[{}]|PROFILE|VIEW|Email to fetch is empty or null", traceId);
//				return buildErrorProfileResponse(response, ResponseCode.USMG_401, "Authentication failed. Unable to resolve user email.", null);
//			}

			Optional<User> userFetch = userRepository.findByIdAndOrganizationId(userId, userDetails.getOrganization().getId());
			if (userFetch.isEmpty()) {
				log.warn("[{}]|PROFILE|VIEW|User not found for email: {}", traceId, userId);
				return buildErrorProfileResponse(response, ResponseCode.USMG_404, "User not found", null);
			}

			User user = userFetch.get();

			Optional<UserTeam> userTeamFetch = userTeamRepository.findFirstByUserId(user.getId());

			ProfileResponseDTO profileDTO = new ProfileResponseDTO();
			profileDTO.setProfilePicture(user.getProfilePictureUrl());
			profileDTO.setName(user.getName());
			profileDTO.setEmail(user.getEmail());
			profileDTO.setPhone(user.getPhone());
			profileDTO.setJobTitle(user.getJobTitle());

			if(userTeamFetch.isPresent()){
				UserTeam userTeam = userTeamFetch.get();
				Team team  = userTeam.getTeam();
				if(team !=null){
					profileDTO.setTeamName(team.getTeamName());
				}
			}

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage(ProfileConstants.PROFILE_FETCH_SUCCESS);
			response.setData(profileDTO);

			log.info("[{}]|PROFILE|VIEW|Profile fetch successful", traceId);
			return response;

		}
		catch (Exception e) {
			log.error("[{}]|PROFILE|VIEW|Exception occurred while fetching profile: {}", traceId, e.getMessage(), e);
			return buildErrorProfileResponse(response, ResponseCode.USMG_500, "Internal server error", null);
		}
	}

	@Override
	public BaseResponse updateProfile(String traceId, ProfileRequestDTO request, UserDetailsImpl userDetails) {
		log.info("[{}]|PROFILE|UPDATE|Start update for user: {}", traceId, userDetails.getUsername());
		BaseResponse response = new BaseResponse();
		response.setTimestamp(new Date());

		try {
			List<ErrorObj> errors = validateProfileRequest(traceId, request);
			if (!errors.isEmpty()) {
				log.warn("[{}]|PROFILE|UPDATE|Validation failed with {} error(s)", traceId, errors.size());
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_400);
				response.setMessage("Validation failed");
				response.setErrors(errors);
				return response;
			}

			User user = userRepository.findByEmail(userDetails.getUsername());
			if (user == null) {
				log.warn("[{}]|PROFILE|UPDATE|User not found: {}", traceId, userDetails.getUsername());
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_404);
				response.setMessage("User not found");
				return response;
			}

			if (request.getName() != null) {
				user.setName(request.getName());
			}

			if (request.getProfilePicture() != null) {
				user.setProfilePictureUrl(request.getProfilePicture());
			}

			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage(ProfileConstants.PROFILE_UPDATE_SUCCESS);
			log.info("[{}]|PROFILE|UPDATE|Profile updated successfully", traceId);
			return response;
		}
		catch (Exception e) {
			log.error("[{}]|PROFILE|UPDATE|Error updating profile: {}", traceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal server error");
			return response;
		}
	}

	private List<ErrorObj> validateProfileRequest(String traceId, ProfileRequestDTO request) {
		List<ErrorObj> errors = new ArrayList<>();
		if (request.getName() != null) {
			if (request.getName().length() > ProfileConstants.MAX_NAME_LENGTH) {
				errors.add(createErrorObj("name", "Invalid.Name", ProfileConstants.INVALID_NAME_LENGTH));
				log.debug("[{}]|VALIDATION|Name length exceeded", traceId);
			}
		}

		if (request.getProfilePicture() != null) {
			if (request.getProfilePicture().length() > ProfileConstants.MAX_PROFILE_PICTURE_LENGTH) {
				errors.add(createErrorObj("profilePicture", "Invalid.ProfilePicture", ProfileConstants.INVALID_PROFILE_PICTURE_LENGTH));
				log.debug("[{}]|VALIDATION|Profile picture URL too long", traceId);
			}
		}
		if (request.getName() != null) {
			if (request.getName().isBlank()) {
				errors.add(createErrorObj("name", "Blank.Name", "Name must not be blank"));
				log.debug("[{}]|VALIDATION|Name is blank", traceId);
			}
			else if (request.getName().length() > ProfileConstants.MAX_NAME_LENGTH) {
				errors.add(createErrorObj("name", "Invalid.Name", ProfileConstants.INVALID_NAME_LENGTH));
				log.debug("[{}]|VALIDATION|Name length exceeded", traceId);
			}
		}

		if (request.getProfilePicture() != null) {
			if (request.getProfilePicture().isBlank()) {
				errors.add(createErrorObj("profilePicture", "Blank.ProfilePicture", "Profile picture must not be blank"));
				log.debug("[{}]|VALIDATION|Profile picture is blank", traceId);
			}
			else if (request.getProfilePicture().length() > ProfileConstants.MAX_PROFILE_PICTURE_LENGTH) {
				errors.add(createErrorObj("profilePicture", "Invalid.ProfilePicture", ProfileConstants.INVALID_PROFILE_PICTURE_LENGTH));
				log.debug("[{}]|VALIDATION|Profile picture URL too long", traceId);
			}
		}

		return errors;
	}

	private ErrorObj createErrorObj(String path, String error, String message) {
		ErrorObj errorObj = new ErrorObj();
		errorObj.setPath(path);
		errorObj.setCode(ResponseCode.USMG_400);
		errorObj.setError(error);
		errorObj.setMessage(message);
		return errorObj;
	}

	private ProfileDataResponse buildErrorProfileResponse(ProfileDataResponse response, String code, String message, ProfileResponseDTO data) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setData(data);
		return response;
	}
}