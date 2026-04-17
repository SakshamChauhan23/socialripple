package com.social.ripple.external_ingestion.implementation;

import com.social.ripple.external_ingestion.service.IContentFetcherService;
import com.social.ripple.external_ingestion.service.IFbContentFetcherService;
import com.social.ripple.external_ingestion.service.IScheduleService;
import com.social.ripple.external_ingestion.service.ShareAnalyticsRefreshService;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Component
@Slf4j
public class ScheduledTask {

	@Autowired
	private IContentFetcherService contentFetcherService;

	@Autowired
	private IFbContentFetcherService fbContentFetcherService;

	@Autowired
	private IScheduleService scheduleService;

	@Autowired
	private ShareAnalyticsRefreshService shareAnalyticsRefreshService;

	// 0 second : every 11 minute : every hour : every day : every month : every day of week
	@Scheduled(cron = "0 */11 * * * *")
	public void runEvery11MinutesWithCron() {
		log.info("Cron task executed at: {}" , new Date());
		contentFetcherService.fetchFromExternalPlatformTwitter(Platform.X);
		// TODO: call the same for every organization id by passing it as param
	}

		@Scheduled(cron = "0 */15 * * * *")
	public void runEvery11MinutesWithCronLinkedinVideoOnly() {
		log.info("Cron task executed at: {}" , new Date());
		contentFetcherService.fetchFromExternalPlatformLinkedinVideoOnly(Platform.LINKEDIN);
		// TODO: call the same for every organization id by passing it as param
	}

	@Scheduled(cron = "0 */11 * * * *")
	public void runEvery11MinutesWithCronForLinkedIn() {
		log.info("Cron task executed at: {}" , new Date());
		contentFetcherService.fetchFromExternalPlatformLinkedIn(Platform.LINKEDIN);
		// TODO: call the same for every organization id by passing it as param
	}

	@Scheduled(cron = "0 */10 * * * *")
	public void runEvery11MinutesWithCronForFacebook() {
		log.info("Cron task executed at: {}" , new Date());
		fbContentFetcherService.fetchFromExternalPlatformFb(Platform.FACEBOOK);
		// TODO: call the same for every organization id by passing it as param
	}

	@Scheduled(cron = "0 */10 * * * *")
	public void runEvery11MinutesWithCronForInstagram() {
		log.info("Cron task executed at: {}" , new Date());
		fbContentFetcherService.fetchFromExternalPlatformInstagram(Platform.INSTAGRAM);
		// TODO: call the same for every organization id by passing it as param
	}

	@Scheduled(cron = "0 */3 * * * *")
	public void runEvery11MinutesWithCronForPostSchedule() {
		log.info("Cron task executed at: {}" , new Date());
		scheduleService.processScheduledPosting();
		// TODO: call the same for every organization id by passing it as param
	}

	@PostConstruct
	public void initShareAnalytics() {
		new Thread(() -> {
			try {
				Thread.sleep(30000); // Wait 30s for app to fully start
				log.info("Initial share analytics backfill starting");
				shareAnalyticsRefreshService.refreshAnalytics();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				log.warn("Initial analytics backfill failed (will retry on next cron): {}", e.getMessage());
			}
		}, "analytics-backfill").start();
	}

	@Scheduled(cron = "0 */30 * * * *")
	public void refreshShareAnalytics() {
		log.info("Share analytics refresh started at: {}", new Date());
		shareAnalyticsRefreshService.refreshAnalytics();
	}

}
