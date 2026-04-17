package com.social.ripple.usermanagement.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalShareRequest {
    private Long postId;
    private Long userId; 
    private List<String> platforms;
}
