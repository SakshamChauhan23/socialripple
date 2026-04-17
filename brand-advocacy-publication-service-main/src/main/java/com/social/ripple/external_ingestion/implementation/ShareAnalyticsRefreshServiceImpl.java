package com.social.ripple.external_ingestion.implementation;

import com.social.ripple.external_ingestion.dao.model.ExternalShare;
import com.social.ripple.external_ingestion.dao.model.ShareAnalytics;
import com.social.ripple.external_ingestion.dao.repository.ExternalShareRepository;
import com.social.ripple.external_ingestion.dao.repository.ShareAnalyticsRepository;
import com.social.ripple.external_ingestion.service.ShareAnalyticsRefreshService;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ShareAnalyticsRefreshServiceImpl implements ShareAnalyticsRefreshService {

    private final ExternalShareRepository externalShareRepository;
    private final ShareAnalyticsRepository shareAnalyticsRepository;
    private final XConnectionServiceImpl xConnectionService;

    public ShareAnalyticsRefreshServiceImpl(ExternalShareRepository externalShareRepository,
                                            ShareAnalyticsRepository shareAnalyticsRepository,
                                            XConnectionServiceImpl xConnectionService) {
        this.externalShareRepository = externalShareRepository;
        this.shareAnalyticsRepository = shareAnalyticsRepository;
        this.xConnectionService = xConnectionService;
    }

    @Override
    public void refreshAnalytics() {
        log.info("share-analytics-refresh|Starting analytics refresh");
        long startTime = System.currentTimeMillis();

        // Find shares that need refresh: no analytics row yet, or sharedAt in last 30 days
        Set<Long> existingAnalyticsShareIds = shareAnalyticsRepository.findAllExternalShareIds();
        List<ExternalShare> allShares = externalShareRepository.findAll();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

        List<ExternalShare> toRefresh = allShares.stream()
                .filter(es -> StringUtils.hasText(es.getExternalPostId()))
                .filter(es -> !existingAnalyticsShareIds.contains(es.getId())
                        || (es.getSharedAt() != null && es.getSharedAt().isAfter(cutoff)))
                .toList();

        log.info("share-analytics-refresh|Total shares: {} | To refresh: {}", allShares.size(), toRefresh.size());

        Set<Platform> unavailablePlatforms = new LinkedHashSet<>();
        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < toRefresh.size(); i++) {
            ExternalShare es = toRefresh.get(i);
            Platform platform = xConnectionService.parsePlatform(es.getPlatform());
            if (platform == null) {
                skipped++;
                continue;
            }

            // Skip platform entirely if it failed earlier in this run
            if (unavailablePlatforms.contains(platform)) {
                skipped++;
                continue;
            }

            try {
                // Use org token for Facebook/Instagram (page tokens needed for insights)
                boolean useOrgToken = (platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM);
                XConnectionServiceImpl.AnalyticsSnapshot snapshot = xConnectionService.fetchAnalyticsSnapshot(
                        new XConnectionServiceImpl.AnalyticsReference(platform, es.getUserId(), es.getExternalPostId(), es.getSharedAt(), es.getTenantId(), useOrgToken),
                        unavailablePlatforms
                );

                if (snapshot != null) {
                    upsert(es, platform, snapshot);
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("share-analytics-refresh|Error refreshing shareId={}: {}", es.getId(), e.getMessage());
                failed++;
            }

            // Rate limit protection: small delay every 10 API calls
            if ((i + 1) % 10 == 0) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("share-analytics-refresh|Completed in {}ms | success={} | failed={} | skipped={} | unavailablePlatforms={}",
                elapsed, success, failed, skipped, unavailablePlatforms);
    }

    private void upsert(ExternalShare es, Platform platform, XConnectionServiceImpl.AnalyticsSnapshot snapshot) {
        ShareAnalytics sa = shareAnalyticsRepository.findByExternalShareId(es.getId())
                .orElseGet(ShareAnalytics::new);

        sa.setExternalShareId(es.getId());
        sa.setPlatform(platform.name());
        sa.setTenantId(es.getTenantId());
        sa.setImpressions(snapshot.impressions);
        sa.setReach(snapshot.reach);
        sa.setLikes(snapshot.likes);
        sa.setComments(snapshot.comments);
        sa.setShares(snapshot.shares);
        sa.setSaves(snapshot.saves);
        sa.setBookmarks(snapshot.bookmarks);
        sa.setRetweets(snapshot.retweets);
        sa.setReplies(snapshot.replies);
        sa.setQuotes(snapshot.quotes);
        sa.setClicks(snapshot.clicks);
        sa.setVideoViews(snapshot.videoViews);
        sa.setEngagements(snapshot.engagements);
        sa.setFetchedAt(LocalDateTime.now());

        shareAnalyticsRepository.save(sa);
    }
}
