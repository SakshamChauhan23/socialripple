package com.social.ripple.loyalty_service.controller;

import com.social.ripple.loyalty_service.constants.ApplicationConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.loyalty_service.dto.request.LoyaltyAwardRequest;
import com.social.ripple.loyalty_service.dto.response.Response;
import com.social.ripple.loyalty_service.service.ILoyaltyAwardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("{version}/loyalty")
public class LoyaltyAwardController {

    private final ILoyaltyAwardService loyaltyAwardService;

    @PostMapping("/award")
    public ResponseEntity<Response> awardPoints(
            @PathVariable("version") String version,
            @RequestHeader(value = "x-trace-id", required = true) String xTraceId,
            @RequestBody LoyaltyAwardRequest request) {
        try {
            log.info("[{}]|LOYALTY|AwardPoints|Start|version={} | userId={} | creditId={}",
                    xTraceId, version, request.getUserId(), request.getCreditId());

            if(request.getCreditId() == null){
                if(request.getIsShare() != null && request.getIsShare()){
                    request.setCreditId(ApplicationConstants.SHARE_POST_CREDIT_ID);
                }else {
                    request.setCreditId(ApplicationConstants.CREATE_POST_CREDIT_ID);
                }
            }


            Response response = loyaltyAwardService.awardPoints(
                    request.getUserId(),
                    request.getCreditId(),
                    xTraceId
            );

            log.info("[{}]|LOYALTY|AwardPoints|End|userId={}", xTraceId, request.getUserId());

            return ResponseEntity.status(response.getHttpRespCode()).body(response);
        } catch (Exception e) {
            log.error("[{}]|LOYALTY|AwardPoints|Error|userId={} | message={}", 
                    xTraceId, request != null ? request.getUserId() : "null", e.getMessage(), e);

            Response errorResponse = new Response();
            errorResponse.setHttpRespCode(500);
            errorResponse.setMessage("Internal Server Error while awarding points");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/estimate")
    public ResponseEntity<Response> estimatePoints(
            @PathVariable("version") String version,
            @RequestHeader(value = "x-trace-id", required = true) String xTraceId,
            @RequestBody LoyaltyAwardRequest request) {
        try {
            log.info("[{}]|LOYALTY|EstimatePoints|Start|version={} | userId={} | creditId={}",
                    xTraceId, version, request.getUserId(), request.getCreditId());

            if(request.getCreditId() == null){
                if(request.getIsShare() != null && request.getIsShare()){
                    request.setCreditId(ApplicationConstants.SHARE_POST_CREDIT_ID);
                }else {
                    request.setCreditId(ApplicationConstants.CREATE_POST_CREDIT_ID);
                }
            }

            Response response = loyaltyAwardService.estimatePoints(
                    request.getUserId(),
                    request.getCreditId(),
                    xTraceId
            );

            log.info("[{}]|LOYALTY|AwardPoints|End|userId={}", xTraceId, request.getUserId());

            return ResponseEntity.status(response.getHttpRespCode()).body(response);
        } catch (Exception e) {
            log.error("[{}]|LOYALTY|AwardPoints|Error|userId={} | message={}",
                    xTraceId, request != null ? request.getUserId() : "null", e.getMessage(), e);

            Response errorResponse = new Response();
            errorResponse.setHttpRespCode(500);
            errorResponse.setMessage("Internal Server Error while awarding points");

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
