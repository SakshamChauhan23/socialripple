package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.util.enumeration.Platform;

public interface IFbContentFetcherService {
    void fetchFromExternalPlatformFb(Platform platform);
    void fetchFromExternalPlatformFbForOrganization(Long organizationId);

    void fetchFromExternalPlatformInstagram(Platform platform);
    void fetchFromExternalPlatformInstagramForOrganization(Long organizationId);

}
