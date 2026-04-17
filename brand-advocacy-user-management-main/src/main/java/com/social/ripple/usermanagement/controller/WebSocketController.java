package com.social.ripple.usermanagement.controller;

import com.social.ripple.usermanagement.dto.request.WebSocketRequest;
import com.social.ripple.usermanagement.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/websocket")
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;


    @PostMapping("/message")
    public ResponseEntity<?> processMessageController(
	    @RequestHeader(value = "x-trace-id", required = true) String xTraceId,
	    @RequestBody WebSocketRequest request) {
	try {
	    webSocketService.processMessage(xTraceId, request);
	    return ResponseEntity.ok(null);
	} catch (Exception e) {
	    log.error("Error | verify phone | message : " + e.getMessage() + e.getClass().getName());
	    return ResponseEntity.status(500).build();
	}

    }
}