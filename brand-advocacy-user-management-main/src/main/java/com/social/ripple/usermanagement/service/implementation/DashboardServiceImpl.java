/**
 * Filename: ContentServiceImpl.java
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

import com.social.ripple.usermanagement.dao.repository.ExternalShareRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.request.ContentGenerateRequestDTO;
import com.social.ripple.usermanagement.dto.response.*;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.GeminiClient;
import com.social.ripple.usermanagement.service.IContentService;
import com.social.ripple.usermanagement.service.IDashboardService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalShareRepository externalShareRepository;

    @Override
    public DashboardResponse fetchEmployeeEngagementMetrics(String traceId, String tenantId, UserDetailsImpl userDetails) {

        Long actualTenantId = userDetails.getOrganization().getId();

        DashboardResponse response = new DashboardResponse();
        EmployeeEngagementDTO engagementDTO = new EmployeeEngagementDTO();
        Double employeeAdoptionPercentage = userRepository.fetchEmployeeAdoptionPercentage(actualTenantId);
        Integer monthlyParticipation = externalShareRepository.fetchEmployeeMonthlyParticipation(actualTenantId);
        Integer totalEngagements = externalShareRepository.fetchEmployeeTotalEngagements(actualTenantId);

        engagementDTO.setEmployeeAdoptionPercentage(employeeAdoptionPercentage);
        engagementDTO.setMonthlyParticipation(monthlyParticipation);
        engagementDTO.setTotalEngagements(totalEngagements);
        // $0.5 to $3 for lower other active $3 to $10
        engagementDTO.setEstimatedMediaValueForCurrentYear((double)totalEngagements * 3.00);
        response.setEmployeeEngagement(engagementDTO);

        return response;
    }

    @Override
    public DashboardResponse fetchEmployeeMonthlyParticipation(String traceId, String tenantId, UserDetailsImpl userDetails) {

        Long actualTenantId = userDetails.getOrganization().getId();

        DashboardResponse response = new DashboardResponse();
        List<MonthlyParticipationDTO> participationDTOs=new ArrayList<>();

        List<Object[]> monthwiseRawList= externalShareRepository.fetchMonthWiseEmployeeParticipation(actualTenantId);

        if(monthwiseRawList != null){
            for(Object[] row: monthwiseRawList){
                participationDTOs.add(new MonthlyParticipationDTO((String)row[1],(String)row[3],(String)row[2],(long)row[4],(String)row[2],(long)row[4]));
            }
        }

        response.setMonthlyParticipation(participationDTOs);

        return response;
    }

    @Override
    public DashboardResponse fetchTopContributors(String traceId, String tenantId, UserDetailsImpl userDetails) {

        Long actualTenantId = userDetails.getOrganization().getId();

        DashboardResponse response = new DashboardResponse();
        List<LeaderDTO> topContributors=new ArrayList<>();

        List<Object[]> contributorRawList= externalShareRepository.fetchTopContributors(actualTenantId);

        if(contributorRawList != null){
            for(Object[] row: contributorRawList){
                topContributors.add(new LeaderDTO((String)row[1],null,(long)row[2]));
            }
        }

        response.setTopContributors(topContributors);

        return response;
    }
}
