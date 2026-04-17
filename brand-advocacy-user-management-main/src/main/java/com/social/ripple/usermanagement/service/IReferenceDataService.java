package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.response.GetAllRolesResponse;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;

public interface IReferenceDataService {

    GetAllRolesResponse getRolesByOrganization(String traceId, String tenantId, Long organizationId);

    GetAllUsersResponse listAllUsersByOrganization(String traceId, String tenantId, Long organizationId);

}
