package com.social.ripple.external_ingestion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BusinessPagesResponse extends BaseResponse {
    private List<OrgBusinessPageDto> businessPages;
    private List<BusinessPageOptionDto> availablePages;
    private String authUrl;
    private String transactionId;
    private boolean selectionRequired;
}
