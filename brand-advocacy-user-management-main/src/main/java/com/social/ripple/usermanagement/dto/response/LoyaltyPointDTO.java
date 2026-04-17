package com.social.ripple.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointDTO {
    private Integer total;
    private Integer xPoints;
    private Integer linkedinPoints;
    private Integer facebookPoints;
    private Integer instagramPoints;
}
