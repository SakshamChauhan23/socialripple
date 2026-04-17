package com.social.ripple.usermanagement.service.implementation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.Role;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.repository.RoleRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.UserDto;
import com.social.ripple.usermanagement.dto.response.GetAllRolesResponse;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;
import com.social.ripple.usermanagement.dto.response.RoleResponse;
import com.social.ripple.usermanagement.service.IReferenceDataService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataServiceImpl implements IReferenceDataService {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;

	@Override
	public GetAllRolesResponse getRolesByOrganization(String traceId, String tenantId, Long organizationId) {
		log.info("[{}]|REFDATA|getRoles|Start|organizationId:{}|tenantId:{}", traceId, organizationId, tenantId);

		GetAllRolesResponse response = new GetAllRolesResponse();
		try {
			List<Role> roles = roleRepository.findAll();

			List<RoleResponse> roleResponses = new ArrayList<>();
			for (Role role : roles) {
				RoleResponse dto = new RoleResponse();
				dto.setId(role.getId());
				dto.setName(role.getName());
				roleResponses.add(dto);
			}

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Roles fetched successfully");
			response.setTimestamp(new Date());
			response.setRoles(roleResponses);

			log.info("[{}]|REFDATA|getRoles|Success|count:{}", traceId, roleResponses.size());
		} catch (Exception e) {
			log.error("[{}]|REFDATA|getRoles|Error|{}", traceId, e.getMessage(), e);

			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Failed to fetch roles");
			response.setTimestamp(new Date());
			response.setRoles(new ArrayList<>());
		}

		return response;
	}

	@Override
	public GetAllUsersResponse listAllUsersByOrganization(String traceId, String tenantId, Long organizationId) {
		log.info("[{}]|REFDATA|listAllUsers|Start|organizationId:{}|tenantId:{}", traceId, organizationId, tenantId);

		GetAllUsersResponse response = new GetAllUsersResponse();
		try {
			List<User> users = userRepository.findByOrganizationId(organizationId);

			List<UserDto> userDtos = new ArrayList<>();
			for (User user : users) {
				userDtos.add(new UserDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
						user.getProfilePictureUrl(), user.getStatus(), user.getIsLeader(),user.getDepartment(), user.getJobTitle(), false));
			}

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Users fetched successfully");
			response.setTimestamp(new Date());
			response.setUsers(userDtos);

			log.info("[{}]|REFDATA|listAllUsers|Success|count:{}", traceId, userDtos.size());
		} catch (Exception e) {
			log.error("[{}]|REFDATA|listAllUsers|Error|{}", traceId, e.getMessage(), e);

			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Failed to fetch users");
			response.setTimestamp(new Date());
			response.setUsers(new ArrayList<>());
		}
		return response;
	}
}