package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.request.ToggleConnectionRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;

public interface IConnectionService {

    /**
     * Toggle the connection of a platform for a user
     * @param userId internal user ID
     * @param request contains platform name and connect/disconnect
     * @param traceId for logging
     * @return BaseResponse indicating success/failure
     */
    BaseResponse toggleConnection(Long userId, ToggleConnectionRequest request, String traceId);
}
