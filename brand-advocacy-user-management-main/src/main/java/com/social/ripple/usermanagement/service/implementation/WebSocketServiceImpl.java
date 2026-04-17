package com.social.ripple.usermanagement.service.implementation;

import com.social.ripple.usermanagement.dto.request.WebSocketRequest;
import com.social.ripple.usermanagement.service.WebSocketService;
import com.social.ripple.usermanagement.util.AppCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
public class WebSocketServiceImpl implements WebSocketService {

	@Autowired
	private AppCache appCache;

    @Override
    public void processMessage(String xtraceId, WebSocketRequest request) throws Exception {
//	SetOperations<String, String> setOps = redisTemplate.opsForSet();
//	Set<String> sessionIds = setOps.members("wsID_" + request.getName());
		List<String> sessionIds = appCache.getWsUserSession(request.getName());
	try {
	    for (String sessionId : sessionIds) {
		WebSocketSession session = WebSocketSessionManager.getSession(sessionId);
		if (session != null && session.isOpen()) {
		    try {
			session.sendMessage(new TextMessage(request.getMessage()));
		    } catch (IOException e) {
				log.error("Error sending WebSocket message for request {}: {}", request.getName(), e.getMessage());
		    }
		}
	    }
	} catch (Exception e) {
	    log.error("Error processing WebSocket message for request {}: {}", request.getName(), e.getMessage());
	}

    }
}