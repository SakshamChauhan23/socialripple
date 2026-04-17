package com.social.ripple.usermanagement.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TrendingTopicsRequestDTO {
	private List<String> topics;
}
