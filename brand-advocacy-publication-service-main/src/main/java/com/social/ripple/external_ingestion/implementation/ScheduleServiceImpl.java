package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.*;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.DashboardResponse;
import com.social.ripple.external_ingestion.dto.response.SchedulePostDTO;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.*;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements IScheduleService {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private XConnectionService xConnectionService;

	@Autowired
	private IFacebookConnectionService facebookConnectionService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PostRepository postRepository;

	@Override
	public DashboardResponse createSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, SchedulePostDTO schedulePostDTO) {
		try{
			Schedule schedule = new Schedule();
			schedule.setContent(schedulePostDTO.getContent());
			schedule.setPostId(schedulePostDTO.getPostId());
			schedule.setScheduledTimeUtc(schedulePostDTO.getScheduledTimeUtc());
			schedule.setType(schedulePostDTO.getType());
			schedule.setUserId(userDetails.getUserId());
			schedule.setTenantId(userDetails.getOrganization().getId());
			schedule.setPlatforms(String.join(",", schedulePostDTO.getPlatforms()));
			scheduleRepository.save(schedule);
		} catch (Exception e) {
			log.error("error in schedule creation");
		}
		return new DashboardResponse();
	}

	@Override
	public DashboardResponse updateSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, SchedulePostDTO schedulePostDTO) {
		DashboardResponse response = new DashboardResponse();
		try {
			Optional<Schedule> scheduleFetch = scheduleRepository.findByIdAndTenantId(
					schedulePostDTO.getId(), userDetails.getOrganization().getId());
			if (scheduleFetch.isEmpty()) {
				log.error("[{}]|SCHEDULE|UPDATE|Schedule not found for id={} tenantId={}", traceId, schedulePostDTO.getId(), userDetails.getOrganization().getId());
				response.setStatus(false);
				response.setCode("PUB_404");
				response.setMessage("Scheduled post not found");
				return response;
			}
			if (Boolean.TRUE.equals(scheduleFetch.get().getIsProcessed())) {
				log.warn("[{}]|SCHEDULE|UPDATE|Already published id={}", traceId, schedulePostDTO.getId());
				response.setStatus(false);
				response.setCode("PUB_409");
				response.setMessage("This post has already been published and cannot be edited");
				return response;
			}
			Schedule schedule = scheduleFetch.get();
			schedule.setContent(schedulePostDTO.getContent());
			schedule.setScheduledTimeUtc(schedulePostDTO.getScheduledTimeUtc());
			schedule.setUserId(userDetails.getUserId());
			schedule.setTenantId(userDetails.getOrganization().getId());
			if (schedulePostDTO.getPlatforms() != null && !schedulePostDTO.getPlatforms().isEmpty()) {
				schedule.setPlatforms(String.join(",", schedulePostDTO.getPlatforms()));
			}
			scheduleRepository.save(schedule);
			log.info("[{}]|SCHEDULE|UPDATE|Success id={}", traceId, schedule.getId());
			response.setStatus(true);
			response.setCode("PUB_200");
			response.setMessage("Schedule updated successfully");
		} catch (Exception e) {
			log.error("[{}]|SCHEDULE|UPDATE|Failed: {}", traceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode("PUB_500");
			response.setMessage("Failed to update schedule");
		}
		return response;
	}

	@Override
	public TweetResponse getUserSchedules(String traceId, String tenantId, UserDetailsImpl userDetails) {
		TweetResponse tweetResponse= new TweetResponse();
		List<SchedulePostDTO> schedulePostDTOs = new ArrayList<>();

		List<Schedule> scheduleList =  scheduleRepository.findByUserIdAndTenantId(userDetails.getUserId(), userDetails.getOrganization().getId());

		if(!scheduleList.isEmpty()){
			List<Long> postIds = scheduleList.stream().map(Schedule::getPostId).toList();

			List<Post> posts = postRepository.findAllById(postIds);
			Map<Long, Post> postIdMap = posts.stream()
					.collect(Collectors.toMap(Post::getId, x -> x));

			for(Schedule schedule:scheduleList){
				SchedulePostDTO schedulePostDTO = new SchedulePostDTO();
				BeanUtils.copyProperties(schedule,schedulePostDTO);
				// Use schedule's own content if edited, otherwise fall back to original post content
				if (!StringUtils.hasText(schedulePostDTO.getContent())) {
					Post post = postIdMap.get(schedulePostDTO.getPostId());
					if (post != null) {
						schedulePostDTO.setContent(post.getContent());
					}
				}

				if(schedulePostDTO.getContent() != null && schedulePostDTO.getContent().length() > 200){
					schedulePostDTO.setContent(schedulePostDTO.getContent().substring(0,95) + "...");
				}

				if(StringUtils.hasText(schedule.getPlatforms())){
					schedulePostDTO.setPlatforms(Arrays.asList(schedule.getPlatforms().split(",")));
				}

				schedulePostDTOs.add(schedulePostDTO);
			}
		}


		tweetResponse.setSchedulePostList(schedulePostDTOs);
		return tweetResponse;
	}

	@Override
	public TweetResponse deleteSchedule(String traceId, String tenantId, UserDetailsImpl userDetails, Long scheduleId) {
		Optional<Schedule> scheduleFetch =  scheduleRepository.findByIdAndTenantId(scheduleId, userDetails.getOrganization().getId());

        scheduleFetch.ifPresent(schedule -> scheduleRepository.delete(
                schedule));

		return new TweetResponse();
	}


	@Override
	public void processScheduledPosting() {
		log.info("Post schedule started current time: {}", LocalDateTime.now());

		// TODO Remove after front end supports UTC properly
		LocalDateTime cutoffTime = LocalDateTime.now(); // .minusMinutes(60*5 + 30);

		log.info("Schedule fetch cutoff time:{}", cutoffTime);

		List<Schedule> schedules = scheduleRepository.findByScheduledTimeUtcBeforeAndIsProcessedFalse(cutoffTime);
		log.info("Number of schedules open:{}", schedules.size());
		if(!schedules.isEmpty()){
			for(Schedule schedule: schedules){
				TweetRequest request = new TweetRequest();

				request.setType(schedule.getType());
				request.setPostId(schedule.getPostId());
				request.setContent(schedule.getContent());
				User user = userRepository.findById(schedule.getUserId()).get();

				UserDetailsImpl userDetails = new UserDetailsImpl(user.getName(), user.getId(), null, false,  Collections.emptyList(),
						user.getOrganization());

				if(schedule.getPlatforms().contains("LINKEDIN")){
					try {
						xConnectionService.createLinkedinPost(request, userDetails, null);
						schedule.setIsProcessed(true);
						schedule.setIsSuccess(true);
						scheduleRepository.save(schedule);
					}catch(Exception e){
						log.error("Linkedin schedule error");
					}
				}
				if(schedule.getPlatforms().contains("X")){
					try {
						xConnectionService.postTweet(request, userDetails);
						schedule.setIsProcessed(true);
						schedule.setIsSuccess(true);
						scheduleRepository.save(schedule);
					}catch(Exception e){
						log.error("Linkedin schedule error");
					}
				}
				if(schedule.getPlatforms().contains("FACEBOOK")){
					try {
						facebookConnectionService.createFbPost(request, userDetails);
						schedule.setIsProcessed(true);
						schedule.setIsSuccess(true);
						scheduleRepository.save(schedule);
					}catch(Exception e){
						log.error("Linkedin schedule error");
					}
				}
				if(schedule.getPlatforms().contains("INSTAGRAM")){
					try {
						facebookConnectionService.createInstagramPost(request, userDetails);
						schedule.setIsProcessed(true);
						schedule.setIsSuccess(true);
						scheduleRepository.save(schedule);
					}catch(Exception e){
						log.error("Linkedin schedule error");
					}
				}
			}
		}
	}
}
