package com.social.ripple.usermanagement.dto;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BusinessPageLinksResponse extends BaseResponse {
    private Map<String, String> businessPageLinks;
    private Map<String, BusinessPageInfoDto> businessPages;
}
