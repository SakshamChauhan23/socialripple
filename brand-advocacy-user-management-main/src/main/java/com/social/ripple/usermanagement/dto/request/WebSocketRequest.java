package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class WebSocketRequest {
    private String name;
    private String message;
}
