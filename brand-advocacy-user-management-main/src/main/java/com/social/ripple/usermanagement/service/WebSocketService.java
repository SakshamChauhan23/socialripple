package com.social.ripple.usermanagement.service;


import com.social.ripple.usermanagement.dto.request.WebSocketRequest;

public interface WebSocketService {
    public void processMessage(String xtraceId, WebSocketRequest request) throws Exception;
}
