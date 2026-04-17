/**
 * Filename: LoyaltyPointsServiceImpl.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.usermanagement.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.LoyaltyPointRepository;
import com.social.ripple.usermanagement.dto.response.LoyaltyResponse;
import com.social.ripple.usermanagement.dto.response.LoyaltyResponseDto;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ILoyaltyPointsService;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Service
public class LoyaltyPointsServiceImpl implements ILoyaltyPointsService {

    private final UserRepository userRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;

    @Autowired
    public LoyaltyPointsServiceImpl(UserRepository userRepository,
                                    LoyaltyPointRepository loyaltyPointRepository) {
        this.userRepository = userRepository;
        this.loyaltyPointRepository = loyaltyPointRepository;
    }


    public LoyaltyResponse getTotalLoyaltyPointsCount(String traceId,
                                                      String tenantId,
                                                      String correlationId,
                                                      String languageId,
                                                      UserDetailsImpl userDetailsImpl, Long userId) {
        log.info("[{}]|LOYALTY|Service|getTotalLoyaltyPointsCount started for userId={}", traceId, userDetailsImpl.getUserId());

        try {
            Long actualUserId = userId== null?userDetailsImpl.getUserId():userId;

            Optional<User> optionalUser = userRepository.findByIdAndOrganizationId(actualUserId,userDetailsImpl.getOrganization().getId());
            if (optionalUser.isEmpty()) {
                log.error("[{}]|LOYALTY|Service|User not found", traceId);
                return LoyaltyResponse.failure("USMG_404", "User not found", null);
            }

            User user = optionalUser.get();

//            if (!user.getOrganization().getId().toString().equals(tenantId)) {
//                log.error("[{}]|LOYALTY|Service|Tenant mismatch. userOrgId={} tenantId={}", traceId, user.getOrganization().getId(), tenantId);
//                return LoyaltyResponse.failure("USMG_403", "Access denied. Tenant mismatch.", null);
//            }

            Integer totalPoints = loyaltyPointRepository.sumPointsByUserId(user.getId());
            if (totalPoints == null) {
                totalPoints = 0;
            }

            LoyaltyResponseDto dto = new LoyaltyResponseDto();
            dto.setUserId(user.getId());
            dto.setLoyaltyPoints(totalPoints);

            log.info("[{}]|LOYALTY|Service|Fetched {} points for userId={}", traceId, totalPoints, user.getId());
            return LoyaltyResponse.success("USMG_200", "Loyalty points fetched successfully", dto);

        } catch (Exception ex) {
            log.error("[{}]|LOYALTY|Service|Exception: {}", traceId, ex.getMessage(), ex);
            return LoyaltyResponse.failure("USMG_500", "Internal server error while fetching loyalty points", ex.getMessage());
        }
    }
}

