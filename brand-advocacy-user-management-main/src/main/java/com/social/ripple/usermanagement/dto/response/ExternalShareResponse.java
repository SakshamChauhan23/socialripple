package com.social.ripple.usermanagement.dto.response;

import java.util.List;

import com.social.ripple.usermanagement.dto.PlatformShareResultDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalShareResponse extends BaseResponse {
    private List<PlatformShareResultDto> results;
}
