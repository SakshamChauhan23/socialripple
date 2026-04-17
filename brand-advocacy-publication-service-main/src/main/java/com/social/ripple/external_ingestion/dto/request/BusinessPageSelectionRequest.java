package com.social.ripple.external_ingestion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessPageSelectionRequest {
    private String transactionId;
    private String selectionId;
}
