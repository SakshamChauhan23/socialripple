package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.social.ripple.usermanagement.dao.model.*;
import com.social.ripple.usermanagement.dao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dto.UserDto;
import com.social.ripple.usermanagement.dto.request.UpdateUserStatusRequest;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;
import com.social.ripple.usermanagement.dto.response.UpdateUserStatusResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IUserService;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

	private final UserRepository userRepository;
	private final InvitationTokenRepository invitationTokenRepository;
	private final UserRoleRepository userRoleRepository;
	private final ScheduleRepository scheduleRepository;

	@Autowired
	private LeaderboardRepository leaderboardRepository;

	@Autowired
	private LoyaltyPointRepository loyaltyPointRepository;

	@Autowired
	private ExternalShareRepository externalShareRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserAuthTokenRepository userAuthTokenRepository;

	@Autowired
	private MediaFileRepository mediaFileRepository;

	@Autowired
	private MediaLibraryRepository mediaLibraryRepository;

	public UserServiceImpl(UserRepository userRepository, InvitationTokenRepository invitationTokenRepository, UserRoleRepository userRoleRepository, ScheduleRepository scheduleRepository) {
		this.userRepository = userRepository;
		this.invitationTokenRepository = invitationTokenRepository;
		this.userRoleRepository =  userRoleRepository;
		this.scheduleRepository = scheduleRepository;
	}

	@Override
	public ResponseEntity<GetAllUsersResponse> getUsersByOrganization(String traceId, Long organizationId,
			String tenantId, UserDetailsImpl userDetails, Pageable pageable) {

		log.info("[{}]|USMG|GetUsers|Start|organizationId:{}|tenantId:{}", traceId, organizationId, tenantId);

		GetAllUsersResponse response = new GetAllUsersResponse();

		try {
			Page<User> users = userRepository.findByOrganizationId(organizationId,pageable);

			List<Long> userIds = users.stream()
					.map(User::getId)
					.toList();

			List<Long> adminUserIds = userRoleRepository.findAdminUserIdsByUserIds(userIds,ApplicationConstants.ADMIN_ROLE);


			List<UserDto> userDtos = new ArrayList<>();
			for (User user : users) {

				// 🔹 Check if user is still in INVITED status
				if (ApplicationConstants.USER_INVITED_STATUS.equals(user.getStatus())) {
				    invitationTokenRepository.findTopByEmailOrderByCreatedAtDesc(user.getEmail()) // ✅ latest token
				        .ifPresent(token -> {
				            if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now()) && !token.getUsed()) {
				                log.info("[{}]|USMG|GetUsers|TokenExpired|userId:{} email:{}",
				                        traceId, user.getId(), user.getEmail());

				                user.setStatus(ApplicationConstants.USER_INVITATION_EXPIRED);
				                userRepository.save(user); // ✅ persist status update
				            }
				        });
				}


				// 🔹 Map User → UserDto
				UserDto dto = new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
						user.getProfilePictureUrl(), user.getStatus(), user.getIsLeader(),user.getDepartment(), user.getJobTitle(), adminUserIds.contains(user.getId()));
				userDtos.add(dto);
			}

			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Users fetched successfully");
			response.setTimestamp(new Date());
			response.setUsers(userDtos);
			response.setTotalElements(users.getTotalElements());
			response.setTotalPages(users.getTotalPages());
			response.setPage(pageable.getPageNumber());

			log.info("[{}]|USMG|GetUsers|Success|count:{}", traceId, userDtos.size());
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|USMG|GetUsers|Error|{}", traceId, e.getMessage(), e);

			response.setStatus(false);
			response.setCode("USMG_500");
			response.setMessage("Failed to fetch users");
			response.setTimestamp(new Date());
			response.setUsers(new ArrayList<>());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@Override
	public ResponseEntity<UpdateUserStatusResponse> ToggleUserStatus(String traceId, Long userId,
			UpdateUserStatusRequest request, String tenantId, UserDetailsImpl userDetails) {

		log.info("[{}]|USMG|ToggleUserStatus|Start|userId:{}|tenantId:{}|enabled:{}", traceId, userId, tenantId,
				request != null ? request.isEnabled() : "null");

		UpdateUserStatusResponse response = new UpdateUserStatusResponse();

		if (request == null) {
			log.warn("[{}]|USMG|ToggleUserStatus|ValidationFailed|Request body is null", traceId);
			response.setStatus(false);
			response.setCode("USMG_400");
			response.setMessage("Request body cannot be null");
			response.setTimestamp(new Date());
			return ResponseEntity.badRequest().body(response);
		}

		if (userId == null) {
			log.warn("[{}]|USMG|ToggleUserStatus|ValidationFailed|UserId is null", traceId);
			response.setStatus(false);
			response.setCode("USMG_400");
			response.setMessage("UserId is required");
			response.setTimestamp(new Date());
			return ResponseEntity.badRequest().body(response);
		}

		try {
			Optional<User> userOpt = userRepository.findById(userId);

			if (userOpt.isEmpty()) {
				log.warn("[{}]|USMG|ToggleUserStatus|UserNotFound|userId:{}", traceId, userId);

				response.setStatus(false);
				response.setCode("USMG_404");
				response.setMessage("User not found with id: " + userId);
				response.setTimestamp(new Date());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			User user = userOpt.get();

			String newStatus = request.isEnabled() ? ApplicationConstants.ACTIVE_USER_STATUS
					: ApplicationConstants.INACTIVE_USER_STATUS;

			if (newStatus.equalsIgnoreCase(user.getStatus())) {
				log.info("[{}]|USMG|ToggleUserStatus|NoChange|userId:{} already {}", traceId, user.getId(), newStatus);

				response.setStatus(true);
				response.setCode("USMG_200");
				response.setMessage("User already in status: " + newStatus);
				response.setTimestamp(new Date());
				response.setUserId(user.getId());
				response.setNewStatus(user.getStatus());
				return ResponseEntity.ok(response);
			}

			user.setStatus(newStatus);
			userRepository.save(user);

			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("User status updated successfully");
			response.setTimestamp(new Date());
			response.setUserId(user.getId());
			response.setNewStatus(user.getStatus());

			log.info("[{}]|USMG|ToggleUserStatus|Success|userId:{}|newStatus:{}", traceId, user.getId(),
					user.getStatus());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|USMG|ToggleUserStatus|Error|{}", traceId, e.getMessage(), e);

			response.setStatus(false);
			response.setCode("USMG_500");
			response.setMessage("Failed to update user status");
			response.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@Override
	public ResponseEntity<UpdateUserStatusResponse> removeUser(String traceId, Long userId, String tenantId, UserDetailsImpl userDetails) {
		UpdateUserStatusResponse response = new UpdateUserStatusResponse();
		try {
			Long adminTenantId = userDetails.getOrganization().getId();
			Optional<User> userOpt = userRepository.findByIdAndOrganizationId(userId,adminTenantId) ;

			if (userOpt.isEmpty()) {
				log.warn("[{}]|USMG|ToggleUserStatus|UserNotFound|userId:{}", traceId, userId);

				response.setStatus(false);
				response.setCode("USMG_404");
				response.setMessage("User not found with id: " + userId);
				response.setTimestamp(new Date());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			User user = userOpt.get();

			// Delete all user related things
			List<Schedule> schedules = scheduleRepository.findByUserIdAndTenantId(userId,adminTenantId);
			if(!CollectionUtils.isEmpty(schedules)){
				scheduleRepository.deleteAll(schedules);
			}


			List<Leaderboard> leaderboards = leaderboardRepository.findByUserId(userId);
			if(!CollectionUtils.isEmpty(leaderboards)) {
				leaderboardRepository.deleteAll(leaderboards);
			}

			List<LoyaltyPoint> loyaltyPoints = loyaltyPointRepository.findByUserId(userId);
			if(!CollectionUtils.isEmpty(loyaltyPoints)) {
				loyaltyPointRepository.deleteAll(loyaltyPoints);
			}

			List<ExternalShare> externalShares = externalShareRepository.findByUserId(userId);
			if(!CollectionUtils.isEmpty(externalShares)) {
				externalShareRepository.deleteAll(externalShares);
			}

			List<Post> posts = postRepository.findByCreatedBy(userId);
			if(!CollectionUtils.isEmpty(posts)) {
				postRepository.deleteAll(posts);
			}

			List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
			if(!CollectionUtils.isEmpty(userRoles)) {
				userRoleRepository.deleteAll(userRoles);
			}

			List<UserAuthToken> userAuthTokens = userAuthTokenRepository.findByUserId(userId);
			if(!CollectionUtils.isEmpty(userAuthTokens)) {
				userAuthTokenRepository.deleteAll(userAuthTokens);
			}

			List<MediaFile> mediaFiles = mediaFileRepository.findByUploadedBy(userId);
			if(!CollectionUtils.isEmpty(mediaFiles)) {
				mediaFileRepository.deleteAll(mediaFiles);
			}

			List<MediaLibrary> mediaLibraries = mediaLibraryRepository.findByCreatedBy(userId);
			if(!CollectionUtils.isEmpty(mediaLibraries)) {
				mediaLibraryRepository.deleteAll(mediaLibraries);
			}

			userRepository.delete(user);

			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("User Deleted successfully");
			response.setTimestamp(new Date());
			response.setUserId(user.getId());
			response.setNewStatus(null);

			log.info("[{}]|USMG|User Removal|Success|userId:{}|newStatus:{}", traceId, user.getId(),
					user.getStatus());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|USMG|User Removal|Error|{}", traceId, e.getMessage(), e);

			response.setStatus(false);
			response.setCode("USMG_500");
			response.setMessage("Failed to update user status");
			response.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
