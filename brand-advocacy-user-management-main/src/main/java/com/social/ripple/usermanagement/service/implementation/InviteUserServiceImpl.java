package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.social.ripple.usermanagement.dto.request.UserAdminToggleRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import com.social.ripple.usermanagement.dao.model.*;
import com.social.ripple.usermanagement.dao.repository.*;
import com.social.ripple.usermanagement.dto.request.InviteUserRequest;
import com.social.ripple.usermanagement.dto.request.ReinviteUserRequest;
import com.social.ripple.usermanagement.dto.response.InviteUserResponse;
import com.social.ripple.usermanagement.service.IInviteUserService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.InviteUserHelperUtil;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class InviteUserServiceImpl implements IInviteUserService {

	private final AppCommonValidator validator;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final RoleRepository roleRepository;
	private final OrganizationRepository organizationRepository;
	private final InvitationTokenRepository invitationTokenRepository;
	private final InviteUserHelperUtil helper;

	@Autowired
	public InviteUserServiceImpl(AppCommonValidator validator, UserRepository userRepository,
			UserRoleRepository userRoleRepository, RoleRepository roleRepository,
			OrganizationRepository organizationRepository, InvitationTokenRepository invitationTokenRepository,
			InviteUserHelperUtil helper) {
		this.validator = validator;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.roleRepository = roleRepository;
		this.organizationRepository = organizationRepository;
		this.invitationTokenRepository = invitationTokenRepository;
		this.helper = helper;
	}

	@Override
	@Transactional
	public ResponseEntity<InviteUserResponse> inviteUser(InviteUserRequest request, String tenantId, String traceId) {
		log.info("[{}]|USMG|InviteUser|Start|tenantId:{}|email:{}", traceId, tenantId,
				request != null ? request.getEmail() : "null");

		try {
			ResponseEntity<InviteUserResponse> validationError = validateInviteOrReinviteRequest(request, tenantId,
					traceId);
			if (validationError != null) {
				return validationError;
			}
			Organization organization = organizationRepository.findById(Long.parseLong(tenantId)).orElse(null);
			if (organization == null) {
				return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_400, "Invalid tenant ID",
						"Organization not found for tenant ID: " + tenantId);
			}
			String token = helper.generateInviteToken(traceId, request.getEmail());
			if (token == null) {
				return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_500, "Token generation failed",
						"Unable to generate invite token");
			}

			// Create user FIRST (fast), send email ASYNC (slow)
			User user = helper.createNewUser(request, organization);
			if (user == null) {
				return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_500, "User creation failed",
						"Unable to create user");
			}
			if (!helper.assignUserRoles(user, request.getRole(), traceId, new InviteUserResponse())) {
				return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_400, "Invalid role",
						"Unable to assign roles to user");
			}

			if (helper.saveInvitationToken(traceId, token, user, organization, request.getRole().get(0)) == null) {
				return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_500, "Token save failed",
						"Unable to save invitation token");
			}

			// Send invitation email ASYNC — don't block the response
			final String emailToken = token;
			final String emailName = request.getName();
			final String orgName = organization.getName();
			CompletableFuture.runAsync(() -> {
				try {
					helper.sendNotification(traceId, request.getEmail(), request.getMobileNumber(), emailToken,
							false, emailName, orgName, null);
				} catch (Exception e) {
					log.error("[{}]|USMG|InviteUser|AsyncEmailFailed|{}", traceId, e.getMessage());
				}
			});

			return helper.buildSuccessResponse(user, token, "User invited successfully",
					"User created, token issued, and invitation mail sent");

		} catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|Exception|{}", traceId, ex.getMessage(), ex);
			return helper.buildErrorResponse("/invite-user", ResponseCode.USMG_500, "Internal server error",
					ex.getMessage());
		}
	}

	@Override
	@Transactional
	public ResponseEntity<InviteUserResponse> reinviteUser(ReinviteUserRequest request, String tenantId,
			String traceId) {
		log.info("[{}]|USMG|ReinviteUser|Start|tenantId:{}|email:{}", traceId, tenantId,
				request != null ? request.getEmail() : "null");

		try {

			ResponseEntity<InviteUserResponse> validationError = validateInviteOrReinviteRequest(request, tenantId,
					traceId);
			if (validationError != null)
				return validationError;

			User user = userRepository.findByEmail(request.getEmail());
			if (user == null) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_404, "User not found",
						"Email not found: " + request.getEmail());
			}

			String status = user.getStatus();
			if (!ApplicationConstants.USER_INVITED_STATUS.equalsIgnoreCase(status)
					&& !ApplicationConstants.USER_INVITATION_EXPIRED.equalsIgnoreCase(status)) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_409, "User already registered",
						"User already active: " + status);
			}

			if (ApplicationConstants.USER_INVITATION_EXPIRED.equalsIgnoreCase(status)) {
				user.setStatus(ApplicationConstants.USER_INVITED_STATUS);
				userRepository.save(user);
			}

			Organization org = user.getOrganization();
			if (org == null) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_500, "Organization missing",
						"No organization associated with user");
			}

			List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
			if (userRoles.isEmpty()) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_400, "Role not found",
						"User has no assigned role");
			}

			Role role = roleRepository.findById(userRoles.get(0).getRoleId()).orElse(null);
			if (role == null) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_400, "Role not found",
						"Invalid role ID: " + userRoles.get(0).getRoleId());
			}

			invitationTokenRepository.markUsedForEmail(user.getEmail());

			String token = helper.generateInviteToken(traceId, user.getEmail());
			if (token == null) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_500, "Token generation failed",
						"Unable to generate reinvite token");
			}

			boolean sent = helper.sendNotification(traceId, user.getEmail(), user.getPhone(), token, true,
					user.getName(), org.getName(), null);
			if (!sent) {
				log.warn("[{}]|USMG|ReinviteUser|NotificationFailed|email:{}", traceId, user.getEmail());
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_500, "Notification failed",
						"Token generated but mail/notification could not be sent. Token not saved.");
			}

			if (helper.saveInvitationToken(traceId, token, user, org, role.getName()) == null) {
				return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_500, "Token save failed",
						"Unable to save reinvite token");
			}

			return helper.buildSuccessResponse(user, token, "User reinvited successfully",
					"Old token(s) marked as used, new token generated, mail sent, and token saved");

		} catch (Exception ex) {
			log.error("[{}]|USMG|ReinviteUser|Exception|{}", traceId, ex.getMessage(), ex);
			return helper.buildErrorResponse("/reinvite-user", ResponseCode.USMG_500, "Internal server error",
					ex.getMessage());
		}
	}

	private ResponseEntity<InviteUserResponse> validateInviteOrReinviteRequest(Object req, String tenantId,
			String traceId) {
		String path = (req instanceof InviteUserRequest) ? "/invite-user" : "/reinvite-user";

		if (req == null) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Invalid request", "Request body is null");
		}

		if (req instanceof InviteUserRequest inviteReq) {
			ResponseEntity<InviteUserResponse> error = validateInviteRequest(inviteReq, traceId, path);
			if (error != null)
				return error;
		} else if (req instanceof ReinviteUserRequest reinviteReq) {
			ResponseEntity<InviteUserResponse> error = validateReinviteRequest(reinviteReq, traceId, path);
			if (error != null)
				return error;
		} else {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Invalid request type",
					"Request must be InviteUserRequest or ReinviteUserRequest");
		}

		if (validator.isNullOrEmpty(traceId, tenantId, "tenantId")) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Missing tenant ID",
					"Header tenant-id is null or empty");
		}

		return null;
	}

	private ResponseEntity<InviteUserResponse> validateInviteRequest(InviteUserRequest inviteReq, String traceId,
			String path) {
		ResponseEntity<InviteUserResponse> emailError = validateEmail(inviteReq.getEmail(), traceId, path);
		if (emailError != null)
			return emailError;

		if (!AppCommonValidator.isValidMobileNumber(inviteReq.getMobileNumber())) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Invalid mobile number",
					"Mobile number format invalid: " + inviteReq.getMobileNumber());
		}

		if (userRepository.existsByEmail(inviteReq.getEmail())) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_409, "User already exists",
					"Email in use: " + inviteReq.getEmail());
		}

		if (inviteReq.getRole() == null || inviteReq.getRole().isEmpty()) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Role is required", "Request.role is empty");
		}

		return null;
	}

	private ResponseEntity<InviteUserResponse> validateReinviteRequest(ReinviteUserRequest reinviteReq, String traceId,
			String path) {
		return validateEmail(reinviteReq.getEmail(), traceId, path);
	}

	private ResponseEntity<InviteUserResponse> validateEmail(String email, String traceId, String path) {
		if (validator.isNullOrEmpty(traceId, email, "Email") || !validator.isValidEmailFormat(traceId, email)) {
			return helper.buildErrorResponse(path, ResponseCode.USMG_400, "Invalid email",
					"Email format invalid: " + email);
		}
		return null;
	}

	@Override
	public ResponseEntity<BaseResponse> updateUser(InviteUserRequest request, String tenantId, String traceId) {

		try{
		if(request.getId() == null){
			throw new RuntimeException("Id is required");
		}

		User user = userRepository.findById(request.getId()).orElseThrow();

		if(request.getName() != null){
			user.setName(request.getName());
		}
		if(request.getDepartment() != null){
			user.setDepartment(request.getDepartment());
		}
		if(request.getJobTitle() != null){
			user.setJobTitle(request.getJobTitle());
		}
		if(request.getMobileNumber() != null){
			user.setPhone(request.getMobileNumber());
		}

		// Duplicate mail id will be handled by DB
		if(request.getEmail() != null){
			user.setEmail(request.getEmail());
		}
		if (request.getProfilePicture() != null) {
			user.setProfilePictureUrl(request.getProfilePicture());
		}

		userRepository.save(user);

		if(!CollectionUtils.isEmpty(request.getRole())){
			List<UserRole> userRoles = userRoleRepository.findByUserId(request.getId());

			userRoleRepository.deleteAll(userRoles);

			if (!helper.assignUserRoles(user, request.getRole(), traceId, new InviteUserResponse())) {
				return helper.buildBaseErrorResponse("/user/update", ResponseCode.USMG_400, "Invalid role",
						"Unable to assign roles to user");
			}
		}

		return helper.buildBaseSuccessResponse(user, null, "User updated successfully",
				"User updated successfully");
		} catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|Exception|{}", traceId, ex.getMessage(), ex);
			return helper.buildBaseErrorResponse("/user/update", ResponseCode.USMG_500, "Internal server error",
					ex.getMessage());
		}
	}

	@Override
	public ResponseEntity<BaseResponse> toggleAdminRole(UserAdminToggleRequest request, String tenantId, String traceId) {
		try{
			if(request.getUserId() == null){
				throw new RuntimeException("UserId is required");
			}

			Role adminRole = roleRepository.findByName(ApplicationConstants.ADMIN_ROLE).orElseThrow();

			UserRole userAdminRole =  userRoleRepository.findFirstByUserIdAndRoleId(request.getUserId(),adminRole.getId());

			String responseMessage = null;

			if(request.getEnableAdmin()){
				if(userAdminRole == null){
					UserRole newAdminRole = new UserRole();
					newAdminRole.setRoleId(adminRole.getId());
					newAdminRole.setUserId(request.getUserId());
					newAdminRole.setAssignedAt(LocalDateTime.now());
					userRoleRepository.save(newAdminRole);

					responseMessage = "Admin Role added successfully";
				}else {
					responseMessage = "Admin Role already present";
				}
			}else {
				if(userAdminRole == null){
					responseMessage = "Admin Role not present";
				}else {
					userRoleRepository.delete(userAdminRole);
					responseMessage = "Admin Role deleted successfully";
				}
			}

			return helper.buildBaseSuccessResponse(null, null, responseMessage,
					responseMessage);
		} catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|Exception|{}", traceId, ex.getMessage(), ex);
			return helper.buildBaseErrorResponse("/user/update", ResponseCode.USMG_500, "Internal server error",
					ex.getMessage());
		}
	}
}
