package com.social.ripple.usermanagement.service.implementation;

import com.social.ripple.usermanagement.util.AppCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {
	@Autowired
	private AppCache appCache;

    public void sendMessageToClient(WebSocketSession session, String messageContent) throws Exception {
	session.sendMessage(new TextMessage(messageContent));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
	String userId = (String) session.getAttributes().get("id");

	if (userId != null) {
	    log.info("WebSocket connection established for user: {}", userId);
	    log.debug("Session ID: {}", session.getId());

	    try {
		appCache.putWsUserSession(userId,session.getId());

		WebSocketSessionManager.addSession(session.getId(), session);
		log.info("Session ID for user {} stored in Redis: {}", userId, session.getId());

	    } catch (Exception e) {
		log.error("Error storing session in Redis for user {}: {}", userId, e.getMessage(), e);
		session.close(CloseStatus.SERVER_ERROR);
		return;
	    }
	} else {
	    log.warn("Error: No username found in WebSocket session.");
	    session.close(CloseStatus.BAD_DATA);
	}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
	String username = (String) session.getAttributes().get("id");

	if (username != null) {
	    try {
//		SetOperations<String, String> setOps = redisTemplate.opsForSet();
//		setOps.remove("wsID_" + username, session.getId());
		appCache.removeWsUserSession(username,session.getId());

		WebSocketSessionManager.removeSession(session.getId());
		log.info("Session ID {} removed from Redis for user id : {}", session.getId(), username);
//		Long remainingSessions = setOps.size("wsID_" + username);
//		if (remainingSessions == 0) {
//		    log.info("No remaining WebSocket sessions for user id : {}", username);
//		    redisTemplate.delete("wsID_" + username);
//		    log.info("Redis entry for user {} deleted due to no active sessions.", username);
//		}

	    } catch (Exception e) {
		log.error("Error removing session from Redis for user id : {} | message : {}", username, e.getMessage(),
			e);
	    }
	} else {
	    log.warn("No username found in WebSocket session for cleanup.");
	}
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
	// TODO Auto-generated method stub

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
	// TODO Auto-generated method stub

    }

    @Override
    public boolean supportsPartialMessages() {
	// TODO Auto-generated method stub
	return false;
    }
}
