package com.social.ripple.usermanagement.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WebSocketHandler webSocketHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor, WebSocketHandler webSocketHandler) {
	this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
	this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
	try {
	    registry.addHandler(webSocketHandler, "/ws").addInterceptors(jwtHandshakeInterceptor)
		    .setAllowedOrigins(allowedOrigins.split(","));

	    log.info("WebSocket handler registered successfully for endpoint /ws");

	} catch (Exception e) {
	    log.error("Error registering WebSocket handler: {}", e.getMessage(), e);
	    throw new RuntimeException("Error registering WebSocket handler", e);
	}
    }
}



//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    private final JwtTokenProvider jwtTokenUtil;
//
//    public WebSocketConfig(JwtTokenProvider jwtTokenUtil) {
//        this.jwtTokenUtil = jwtTokenUtil;
//    }
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/queue", "/topic"); // Enable the simple broker
//        config.setApplicationDestinationPrefixes("/app"); // Set the application prefix
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/ws")
//                .addInterceptors(new JwtHandshakeInterceptor2(jwtTokenUtil)) // Add the JWT interceptor
//                .setAllowedOrigins("http://localhost:8084/")
//                .withSockJS();
//    }
//}
