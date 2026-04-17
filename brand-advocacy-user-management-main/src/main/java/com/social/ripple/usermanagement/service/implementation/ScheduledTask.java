package com.social.ripple.usermanagement.service.implementation;

import com.social.ripple.usermanagement.service.IContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class ScheduledTask {

	@Autowired
	private IContentService contentService;


	// 0 second : 0 minute : every 5.00  : every day : every month : every day of week
	@Scheduled(cron = "0 0 5 * * *")
	public void runEvery11MinutesWithCron() {
		log.info("Cron task executed at: {}" , new Date());
		contentService.fetchTrendingTopics("job-call");
	}

}
