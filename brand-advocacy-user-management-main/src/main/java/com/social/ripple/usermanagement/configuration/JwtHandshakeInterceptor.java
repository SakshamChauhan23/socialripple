package com.social.ripple.usermanagement.configuration;

import com.social.ripple.usermanagement.security.jwt.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired 
    private JwtUtils jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
	    Map<String, Object> attributes) throws Exception {
	String query = request.getURI().getQuery();
	String token = null;
	try {
	    if (query != null) {
		for (String param : query.split("&")) {
		    String[] keyValue = param.split("=");
		    if (keyValue.length == 2 && "token".equalsIgnoreCase(keyValue[0])) {
			token = keyValue[1];
			break;
		    }
		}
	    }
	    if (token == null) {
		log.error("Token is missing in the request.");
		return false;
	    }
	    if (!jwtTokenProvider.validateAccessToken("ws-trc1",token)) {
		log.error("Invalid token: {}", token);
		return false;
	    }
	    String userId = jwtTokenProvider.getUserIdFromActiveToken("ws-trc2",token);
	    attributes.put("id", userId);
	    log.info("Handshake successful for user id : {}", userId);
	    return true;
	} catch (Exception e) {
	    log.error("Error during WebSocket handshake: {}", e.getMessage(), e);
	    throw new RuntimeException("Error during WebSocket handshake", e);
	}
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
	    Exception exception) {
	if (exception != null) {
	    log.error("Error after handshake: {}", exception.getMessage(), exception);
	} else {
	    log.info("Handshake completed successfully.");
	}
    }
}
