package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.util.enumeration.Platform;

public interface IContentFetcherService {
    void fetchFromExternalPlatformTwitter(Platform platform);
    void fetchFromExternalPlatformTwitterForOrganization(Long organizationId);

    void fetchFromExternalPlatformLinkedinVideoOnly(Platform platform);

    void fetchFromExternalPlatformLinkedIn(Platform platform);
    void fetchFromExternalPlatformLinkedInForOrganization(Long organizationId);

}
