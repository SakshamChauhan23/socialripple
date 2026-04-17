package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.util.enumeration.Platform;

public interface IPostDeletionCheckerService {
    void performDeletionCheckForLinkedIn(Platform platform);

}
