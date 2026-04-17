package com.social.ripple.external_ingestion.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.IContentFetcherService;
import com.social.ripple.external_ingestion.service.IFbContentFetcherService;
import com.social.ripple.external_ingestion.service.ILeaderPostFetcherService;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.constants.ResponseCode;
import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dto.response.BaseResponse;
import com.social.ripple.external_ingestion.dto.response.ErrorObj;
import com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService;
import com.social.ripple.external_ingestion.service.XConnectionService;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/x")
public class XConnectionController {

	private final XConnectionService xConnectionService;
	private final IContentFetcherService contentFetcherService;
	private final IFbContentFetcherService fbContentFetcherService;
	private final ILeaderPostFetcherService leaderPostFetcherService;
	private final AppCache appCache;
	private final PublicUrlProperties publicUrlProperties;
	private final OrganizationPlatformSettingsService organizationPlatformSettingsService;

	private static final long TRANSACTION_TTL_MINUTES = 10;

	@GetMapping("/auth")
	public ResponseEntity<BaseResponse> auth(@RequestParam("transactionId") String transactionId,
	                                         HttpServletResponse response, HttpSession session) {
	    try {
	        UserAuthToken token = xConnectionService.getTokenByTransactionId(transactionId);

	        if (token == null) {
	            log.error("Auth | transactionId={} not found", transactionId);
	            return ResponseEntity.status(404)
	                    .body(buildResponse(false, ResponseCode.MS_INVALID_TRANSACTION, "Transaction not found",
	                            "Invalid transactionId",
	                            new ErrorObj("/api/x/auth", ResponseCode.MS_INVALID_TRANSACTION,
	                                    "Transaction not found", "Invalid transactionId")));
	        }

	        if (isTransactionExpired(token.getTransactionCreatedAt())) {
	            log.warn("Auth | transactionId={} expired", transactionId);
	            return ResponseEntity.status(410)
	                    .body(buildResponse(false, ResponseCode.MS_TOKEN_EXPIRED, "Transaction expired", "TTL exceeded",
	                            new ErrorObj("/api/x/auth", ResponseCode.MS_TOKEN_EXPIRED, "Transaction expired",
	                                    "TTL exceeded")));
	        }

	        // --- FIX START ---
	        String callbackBaseUrl = publicUrlProperties.getXCallbackUrl();
	        if (callbackBaseUrl == null || callbackBaseUrl.isBlank()) {
	            log.error("X callback URL not configured");
	            return ResponseEntity.status(500)
	                    .body(buildResponse(false, ResponseCode.MS_500, "Callback URL not configured",
	                            "X callback URL property is missing",
	                            new ErrorObj("/api/x/auth", ResponseCode.MS_500,
	                                    "Callback URL not configured", "X callback URL property is missing")));
	        }

	        // Ensure callbackBaseUrl has proper scheme
	        if (!callbackBaseUrl.startsWith("http://") && !callbackBaseUrl.startsWith("https://")) {
	            log.error("X callback URL does not start with http/https: {}", callbackBaseUrl);
	            return ResponseEntity.status(500)
	                    .body(buildResponse(false, ResponseCode.MS_500, "Invalid callback URL",
	                            "X callback URL must start with http:// or https://",
	                            new ErrorObj("/api/x/auth", ResponseCode.MS_500,
	                                    "Invalid callback URL", "X callback URL must start with http:// or https://")));
	        }

	        String callbackUrl = callbackBaseUrl;

	        log.info("Generated callback URL: {}", callbackUrl);

	        // --- FIX END ---

	        String authUrl = xConnectionService.getAuthorizationUrl(callbackUrl, token, session);
	        log.info("Redirecting to Twitter/X auth URL for transactionId={}", transactionId);

	        response.sendRedirect(authUrl);
	        return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Redirecting to OAuth URL", null, null));

	    } catch (Exception e) {
	        log.error("Error | auth | transactionId={} message={}", transactionId, e.getMessage(), e);
	        return ResponseEntity.status(500).body(buildResponse(false, ResponseCode.MS_500, "Auth error",
	                e.getMessage(), new ErrorObj("/api/x/auth", ResponseCode.MS_500, "Auth error", e.getMessage())));
	    }
	}


	@GetMapping("/callback")
	public void callback(@RequestParam("oauth_verifier") String oauthVerifier,
			@RequestParam(value = "oauth_token", required = false) String oauthToken,
			@RequestParam(value = "transactionId", required = false) String transactionId,
			HttpServletResponse response, HttpSession session) {
		String resolvedTransactionId = transactionId;
		try {
			UserAuthToken token = null;
			if (resolvedTransactionId != null && !resolvedTransactionId.isBlank()) {
				token = xConnectionService.getTokenByTransactionId(resolvedTransactionId);
			}
			if (token == null && oauthToken != null && !oauthToken.isBlank()) {
				token = xConnectionService.getPendingTokenByRequestToken(oauthToken);
				if (token != null) {
					resolvedTransactionId = token.getTransactionId();
				}
			}

			String orgTransactionId = firstNonBlank(
					resolvedTransactionId,
					organizationPlatformSettingsService.resolveActiveBusinessConnectTransactionIdByRequestToken(oauthToken, Platform.X)
			);

			if (token == null && orgTransactionId != null && !orgTransactionId.isBlank()) {
				organizationPlatformSettingsService.completeSharedXBusinessConnect(oauthVerifier, oauthToken, orgTransactionId, session);
				response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
						+ "?businessConnect=select&platform=x&transactionId=" + URLEncoder.encode(orgTransactionId, StandardCharsets.UTF_8));
				return;
			}

			if (token == null) {
				log.error("Callback | oauthToken={} | transactionId={} not found", oauthToken, resolvedTransactionId);
				redirectWithStatus(response, null, "error", "invalid_transaction");
				return;
			}

			if (isTransactionExpired(token.getTransactionCreatedAt())) {
				log.warn("Callback | transactionId={} expired", resolvedTransactionId);
				redirectWithStatus(response, token, "error", "transaction_expired");
				return;
			}

			UserAuthToken savedToken = xConnectionService.handleCallback(oauthVerifier, token, session, resolvedTransactionId);

			savedToken.setTransactionStatus(TransactionStatus.COMPLETED);
			savedToken.setTransactionId(null);
			savedToken.setTransactionCreatedAt(null);
			String originalSourcePage = savedToken.getOauthSourcePage();
			savedToken.setOauthSourcePage("PERSONAL");
			xConnectionService.saveToken(savedToken);

			log.info("OAuth completed for userId={} transactionId={}", savedToken.getUserId(), resolvedTransactionId);

			redirectToSettings(response, originalSourcePage, "success", null);

		} catch (Exception e) {
			log.error("Error | callback | oauthToken={} | transactionId={} | message={}", oauthToken, resolvedTransactionId, e.getMessage(), e);
			try {
				// Check if this was a personal token flow (token found earlier) or org flow
				boolean isPersonalFlow = (resolvedTransactionId != null && !resolvedTransactionId.isBlank());
				String orgOnlyTransactionId = organizationPlatformSettingsService
						.resolveActiveBusinessConnectTransactionIdByRequestToken(oauthToken, Platform.X);

				if (!isPersonalFlow && orgOnlyTransactionId != null && !orgOnlyTransactionId.isBlank()) {
					response.sendRedirect(publicUrlProperties.getFrontendAdminSettingsUrl()
							+ "?businessConnect=error&platform=x&reason=token_exchange_failed");
					return;
				}
				redirectWithStatus(response, null, "error", "token_exchange_failed");
			} catch (Exception ex) {
				log.error("Error sending error response: {}", ex.getMessage(), ex);
				try {
					redirectWithStatus(response, null, "error", "token_exchange_failed");
				} catch (Exception ignored) {
					// nothing more we can do
				}
			}
		}
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

	private void redirectWithStatus(HttpServletResponse response, UserAuthToken token, String status, String reason)
			throws java.io.IOException {
		redirectToSettings(response, token != null ? token.getOauthSourcePage() : null, status, reason);
	}

	private void redirectToSettings(HttpServletResponse response, String sourcePage, String status, String reason)
			throws java.io.IOException {
		String redirectUrl = publicUrlProperties.resolveFrontendSettingsUrl(sourcePage)
				+ "?socialConnect=" + status
				+ "&platform=x";
		if (reason != null && !reason.isBlank()) {
			redirectUrl += "&reason=" + reason.toLowerCase(Locale.ROOT);
		}
		response.sendRedirect(redirectUrl);
	}

	@PostMapping("/post")
	public ResponseEntity<?> postTweet(@RequestHeader(value = "x-trace-id", required = true) String xTraceId,
									   			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody  TweetRequest request) {
		try {
			TweetResponse response = xConnectionService.postTweet(request, userDetails);
			if (response.isStatus()) {
				return ResponseEntity.ok(response);
			}
			return ResponseEntity.badRequest().body(response);
		} catch (Exception e) {
			log.error("Error | tweet-with-media | traceId={} | message={}", xTraceId, e.getMessage(), e);
			return ResponseEntity.status(500).body(buildTweetFailureResponse(
					ResponseCode.MS_500,
					"X post failed",
					e.getMessage(),
					"/v1/api/x/post",
					e.getClass().getSimpleName()));
		}
	}

	private boolean isTransactionExpired(LocalDateTime transactionCreatedAt) {
		return transactionCreatedAt != null
				&& transactionCreatedAt.plusMinutes(TRANSACTION_TTL_MINUTES).isBefore(LocalDateTime.now());
	}

	private BaseResponse buildResponse(boolean status, String code, String message, String devMessage, ErrorObj error) {
		BaseResponse response = new BaseResponse();
		response.setStatus(status);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(error != null ? Collections.singletonList(error) : null);
		return response;
	}

	private TweetResponse buildTweetFailureResponse(String code, String message, String devMessage, String path, String errorMessage) {
		TweetResponse response = new TweetResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(Collections.singletonList(new ErrorObj(path, code, errorMessage, devMessage)));
		return response;
	}

	@GetMapping("/fetch/x")
	public ResponseEntity<BaseResponse> fetch(HttpServletResponse response, HttpSession session) {

			contentFetcherService.fetchFromExternalPlatformTwitter(Platform.X);

			return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching X", null, null));

	}

	@GetMapping("/fetch/linkedin")
	public ResponseEntity<BaseResponse> fetchLinkedIn(HttpServletResponse response, HttpSession session) {

		contentFetcherService.fetchFromExternalPlatformLinkedIn(Platform.LINKEDIN);

		return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching Linkedin", null, null));

	}

	@GetMapping("/fetch/linkedin-v")
	public ResponseEntity<BaseResponse> fetchLinkedinV(HttpServletResponse response, HttpSession session) {

		contentFetcherService.fetchFromExternalPlatformLinkedinVideoOnly(Platform.LINKEDIN);

		return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching X", null, null));

	}

	@GetMapping("/fetch/facebook")
	public ResponseEntity<BaseResponse> fetchFacebook(HttpServletResponse response, HttpSession session) {

		fbContentFetcherService.fetchFromExternalPlatformFb(Platform.FACEBOOK);

		return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching FB", null, null));

	}

	@GetMapping("/fetch/instagram")
	public ResponseEntity<BaseResponse> fetchInstagram(HttpServletResponse response, HttpSession session) {

		fbContentFetcherService.fetchFromExternalPlatformInstagram(Platform.INSTAGRAM);

		return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching Instagram", null, null));

	}

	@GetMapping("/fetch/x-leader")
	public ResponseEntity<BaseResponse> fetchTwitterLeader(HttpServletResponse response, HttpSession session) {

		leaderPostFetcherService.fetchLeadPostFromExternalPlatformTwitter(Platform.X);

		return ResponseEntity.ok(buildResponse(true, ResponseCode.MS_200, "Fetching X Leader", null, null));

	}

}
