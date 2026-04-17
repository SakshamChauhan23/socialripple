package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.dto.response.DashboardResponse;
import com.social.ripple.external_ingestion.dto.response.SchedulePostDTO;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.util.enumeration.Platform;

public interface IScheduleService {

    DashboardResponse createSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, SchedulePostDTO schedulePostDTO);

    DashboardResponse updateSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, SchedulePostDTO schedulePostDTO);

    TweetResponse getUserSchedules(String traceId, String tenantId, UserDetailsImpl userDetails);

    TweetResponse deleteSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, Long scheduleId);

    void processScheduledPosting();
}
