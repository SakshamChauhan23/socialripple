package com.social.ripple.external_ingestion.implementation;

import com.social.ripple.external_ingestion.service.BusinessPageFetchRequestedEvent;
import com.social.ripple.external_ingestion.service.IContentFetcherService;
import com.social.ripple.external_ingestion.service.IFbContentFetcherService;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessPageFetchRequestedListener {

    private final IContentFetcherService contentFetcherService;
    private final IFbContentFetcherService fbContentFetcherService;

    @Async
    @EventListener
    public void onBusinessPageFetchRequested(BusinessPageFetchRequestedEvent event) {
        if (event == null || event.organizationId() == null || event.platform() == null) {
            return;
        }

        try {
            switch (event.platform()) {
                case FACEBOOK -> fbContentFetcherService.fetchFromExternalPlatformFbForOrganization(event.organizationId());
                case INSTAGRAM -> fbContentFetcherService.fetchFromExternalPlatformInstagramForOrganization(event.organizationId());
                case LINKEDIN -> contentFetcherService.fetchFromExternalPlatformLinkedInForOrganization(event.organizationId());
                case X -> contentFetcherService.fetchFromExternalPlatformTwitterForOrganization(event.organizationId());
            }
        } catch (Exception e) {
            log.error("Immediate business-page fetch failed for orgId={} platform={}", event.organizationId(), event.platform(), e);
        }
    }
}
