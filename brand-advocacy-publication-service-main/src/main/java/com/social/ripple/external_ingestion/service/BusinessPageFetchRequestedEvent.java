package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.util.enumeration.Platform;

public record BusinessPageFetchRequestedEvent(Long organizationId, Platform platform) {
}
