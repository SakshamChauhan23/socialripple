package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.request.ExternalShareRequest;
import com.social.ripple.usermanagement.dto.response.ExternalShareResponse;

public interface IExternalShareService {
    ExternalShareResponse sharePostExternally(Long userId, ExternalShareRequest request, String traceId);
}
