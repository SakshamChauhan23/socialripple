package com.social.ripple.external_ingestion.controller;

import com.social.ripple.external_ingestion.constants.ResponseCode;
import com.social.ripple.external_ingestion.dto.response.BaseResponse;
import com.social.ripple.external_ingestion.dto.request.BusinessPageSelectionRequest;
import com.social.ripple.external_ingestion.dto.request.OrgBusinessPageRequest;
import com.social.ripple.external_ingestion.dto.response.BusinessPagesResponse;
import com.social.ripple.external_ingestion.dto.response.ErrorObj;
import com.social.ripple.external_ingestion.dto.response.LeaderPagesResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/org")
public class OrganizationPlatformSettingsController {

    private final OrganizationPlatformSettingsService organizationPlatformSettingsService;
    private final PublicUrlProperties publicUrlProperties;

    @GetMapping("/business-pages")
    public ResponseEntity<BusinessPagesResponse> getBusinessPages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Business pages fetched successfully");
        response.setTimestamp(new Date());
        response.setBusinessPages(organizationPlatformSettingsService.getBusinessPages(userDetails));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/business-pages/{platform}")
    public ResponseEntity<BusinessPagesResponse> saveBusinessPage(@PathVariable("platform") String platform,
                                                                  @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                  @RequestBody OrgBusinessPageRequest request) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Business page saved successfully");
        response.setTimestamp(new Date());
        response.setBusinessPages(Collections.singletonList(
                organizationPlatformSettingsService.saveBusinessPage(userDetails, parsePlatform(platform), request)
        ));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/business-pages/{platform}")
    public ResponseEntity<BusinessPagesResponse> deleteBusinessPage(@PathVariable("platform") String platform,
                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        organizationPlatformSettingsService.deleteBusinessPage(userDetails, parsePlatform(platform));
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_204);
        response.setMessage("Business page disconnected successfully");
        response.setTimestamp(new Date());
        response.setBusinessPages(Collections.emptyList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/business-pages/{platform}/connect")
    public ResponseEntity<BusinessPagesResponse> connectBusinessPage(@PathVariable("platform") String platform,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                     HttpSession session) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Business page authorization URL generated");
        response.setTimestamp(new Date());
        response.setAuthUrl(organizationPlatformSettingsService.startBusinessConnect(userDetails, parsePlatform(platform), session));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/business-pages/{platform}/connect/start")
    public ResponseEntity<BusinessPagesResponse> startBusinessPageConnect(@PathVariable("platform") String platform,
                                                                          @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                          HttpSession session) {
        return connectBusinessPage(platform, userDetails, session);
    }

    @PostMapping("/business-pages/{platform}/manual")
    public ResponseEntity<BusinessPagesResponse> saveBusinessPageManual(@PathVariable("platform") String platform,
                                                                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                        @RequestBody OrgBusinessPageRequest request) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Business page saved successfully");
        response.setTimestamp(new Date());
        response.setBusinessPages(Collections.singletonList(
                organizationPlatformSettingsService.saveBusinessPageManual(userDetails, parsePlatform(platform), request)
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/business-pages/{platform}/available-pages")
    public ResponseEntity<BusinessPagesResponse> getAvailableBusinessPages(@PathVariable("platform") String platform,
                                                                           @RequestParam("transactionId") String transactionId,
                                                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Available business pages fetched successfully");
        response.setTimestamp(new Date());
        response.setTransactionId(transactionId);
        response.setAvailablePages(organizationPlatformSettingsService.getAvailableBusinessPages(userDetails, parsePlatform(platform), transactionId));
        response.setSelectionRequired(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/business-pages/{platform}/select")
    public ResponseEntity<BusinessPagesResponse> selectBusinessPage(@PathVariable("platform") String platform,
                                                                    @AuthenticationPrincipal UserDetailsImpl userDetails,
                                                                    @RequestBody BusinessPageSelectionRequest request) {
        BusinessPagesResponse response = new BusinessPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Business page connected successfully");
        response.setTimestamp(new Date());
        response.setBusinessPages(Collections.singletonList(
                organizationPlatformSettingsService.selectBusinessPage(userDetails, parsePlatform(platform), request)
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/business-pages/{platform}/callback")
    public void businessPageCallback(@PathVariable("platform") String platform,
                                     @RequestParam(value = "code", required = false) String code,
                                     @RequestParam(value = "state", required = false) String state,
                                     @RequestParam(value = "oauth_token", required = false) String oauthToken,
                                     @RequestParam(value = "oauth_verifier", required = false) String oauthVerifier,
                                     @RequestParam(value = "transactionId", required = false) String transactionId,
                                     HttpServletResponse response,
                                     HttpSession session) throws java.io.IOException {
        Platform parsedPlatform = parsePlatform(platform);
        try {
            if ((parsedPlatform == Platform.FACEBOOK || parsedPlatform == Platform.INSTAGRAM)
                    && (code == null || code.isBlank() || state == null || state.isBlank())) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Missing business page callback parameters");
            }
            if (parsedPlatform == Platform.LINKEDIN && (code == null || code.isBlank() || state == null || state.isBlank())) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Missing LinkedIn business page callback parameters");
            }
            if (parsedPlatform == Platform.X
                    && (oauthVerifier == null || oauthVerifier.isBlank()
                    || ((oauthToken == null || oauthToken.isBlank()) && (transactionId == null || transactionId.isBlank())))) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Missing X business page callback parameters");
            }

            // Resolve X transaction ID BEFORE completeBusinessConnect (which overwrites the request token)
            String resolvedTransactionId = null;
            if (parsedPlatform == Platform.X) {
                resolvedTransactionId = firstNonBlank(transactionId,
                        organizationPlatformSettingsService.resolveActiveBusinessConnectTransactionIdByRequestToken(oauthToken, Platform.X));
            }

            boolean selectionRequired = organizationPlatformSettingsService.completeBusinessConnect(
                    parsedPlatform,
                    code,
                    state,
                    oauthToken,
                    oauthVerifier,
                    transactionId,
                    session
            );
            if (selectionRequired) {
                if (parsedPlatform != Platform.X) {
                    resolvedTransactionId = state;
                }
                response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
                        + "?businessConnect=select&platform=" + parsedPlatform.name().toLowerCase(Locale.ROOT)
                        + "&transactionId=" + URLEncoder.encode(resolvedTransactionId, StandardCharsets.UTF_8));
            } else {
                response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
                        + "?businessConnect=success&platform=" + parsedPlatform.name().toLowerCase(Locale.ROOT));
            }
        } catch (ResponseStatusException ex) {
            log.error("Business page callback failed for platform={}", parsedPlatform, ex);
            response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
                    + "?businessConnect=error&platform=" + parsedPlatform.name().toLowerCase(Locale.ROOT)
                    + "&reason=" + URLEncoder.encode(resolveBusinessConnectReason(ex), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Business page callback failed unexpectedly for platform={} | transactionId={} | oauthTokenPresent={} | oauthVerifierPresent={}",
                    parsedPlatform, transactionId, oauthToken != null && !oauthToken.isBlank(), oauthVerifier != null && !oauthVerifier.isBlank(), ex);
            response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
                    + "?businessConnect=error&platform=" + parsedPlatform.name().toLowerCase(Locale.ROOT)
                    + "&reason=" + URLEncoder.encode("business_connect_failed", StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/leader-pages")
    public ResponseEntity<LeaderPagesResponse> getLeaderPages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        LeaderPagesResponse response = new LeaderPagesResponse();
        response.setStatus(true);
        response.setCode(ResponseCode.MS_200);
        response.setMessage("Leader page status fetched successfully");
        response.setTimestamp(new Date());
        response.setLeaderPages(organizationPlatformSettingsService.getLeaderPages(userDetails));
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<BaseResponse> handleStatusException(ResponseStatusException ex) {
        BaseResponse response = new BaseResponse();
        response.setStatus(false);
        response.setCode(mapCode(ex));
        response.setMessage(ex.getReason());
        response.setDevMessage(ex.getReason());
        response.setTimestamp(new Date());
        response.setErrors(Collections.singletonList(new ErrorObj("/v1/api/org", response.getCode(), ex.getReason(), ex.getReason())));
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Platform parsePlatform(String platform) {
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        if ("TWITTER".equals(normalized)) {
            return Platform.X;
        }
        return Platform.valueOf(normalized);
    }

    private String mapCode(ResponseStatusException ex) {
        return switch (ex.getStatusCode().value()) {
            case 403 -> ResponseCode.MS_403;
            case 404 -> ResponseCode.MS_404;
            default -> ResponseCode.MS_400;
        };
    }

    private String resolveBusinessConnectReason(ResponseStatusException ex) {
        String reason = ex.getReason();
        if (reason == null) {
            return "business_connect_failed";
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("expired")) {
            return "transaction_expired";
        }
        if (normalized.contains("transaction")) {
            return "invalid_transaction";
        }
        if (normalized.contains("callback token mismatch")) {
            return "invalid_transaction";
        }
        if (normalized.contains("token exchange")) {
            return "token_exchange_failed";
        }
        if (normalized.contains("discovery")) {
            return "page_discovery_failed";
        }
        if (normalized.contains("no facebook pages found")
                || normalized.contains("no instagram business accounts found")
                || normalized.contains("no linkedin organization pages found")) {
            return "no_business_pages_found";
        }
        if (normalized.contains("personal") && normalized.contains("not allowed")) {
            return "personal_account_not_allowed";
        }
        if (normalized.contains("manual company setup")) {
            return "manual_setup_required";
        }
        if (normalized.contains("not supported")) {
            return "unsupported_platform";
        }
        return "business_connect_failed";
    }
}
