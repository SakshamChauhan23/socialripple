package com.social.ripple.external_ingestion.controller;

import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.constants.ResponseCode;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.BaseResponse;
import com.social.ripple.external_ingestion.dto.response.ErrorObj;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.IFacebookConnectionService;
import com.social.ripple.external_ingestion.service.XConnectionService;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/instagram")
public class InstagramConnectionController {

	private final IFacebookConnectionService facebookConnectionService;
	private final XConnectionService xConnectionService;
	private final AppCache appCache;
	private final PublicUrlProperties publicUrlProperties;

	private static final long TRANSACTION_TTL_MINUTES = 10;

	@GetMapping("/callback")
	public void callback(@RequestParam("code") String oauthVerifier,
						 @RequestParam("state") String transactionId, HttpServletResponse response, HttpSession session) {
		UserAuthToken token = null;
		try {
			token = xConnectionService.getTokenByTransactionId(transactionId);

			if (token == null) {
				log.error("Callback | transactionId={} not found", transactionId);
				redirectWithStatus(response, null, "error", "invalid_transaction");
				return;
			}

			if (isTransactionExpired(token.getTransactionCreatedAt())) {
				log.warn("Callback | transactionId={} expired", transactionId);
				redirectWithStatus(response, token, "error", "transaction_expired");
				return;
			}


			UserAuthToken savedToken = facebookConnectionService.handleInstagramCallback(oauthVerifier, token, session, transactionId);

			savedToken.setTransactionStatus(TransactionStatus.COMPLETED);
			savedToken.setTransactionId(null);
			savedToken.setTransactionCreatedAt(null);
			String originalSourcePage = savedToken.getOauthSourcePage();
			savedToken.setOauthSourcePage("PERSONAL");
			facebookConnectionService.saveToken(savedToken);

			log.info("OAuth completed for userId={} transactionId={}", savedToken.getUserId(), transactionId);

			redirectToSettings(response, originalSourcePage, "success", null);

		} catch (Exception e) {
			log.error("Error | callback | transactionId={} message={}", transactionId, e.getMessage(), e);
			try {
				redirectWithStatus(response, token, "error", resolveFailureReason(e));
			} catch (Exception ex) {
				log.error("Error sending error response: {}", ex.getMessage(), ex);
			}
		}
	}

	private String resolveFailureReason(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ResponseStatusException responseStatusException) {
				String reason = responseStatusException.getReason();
				if (reason != null) {
					if (reason.contains("Meta app credentials are not configured")) {
						return "meta_app_not_configured";
					}
					if (reason.contains("No Instagram business accounts found")) {
						return "no_instagram_business_accounts_found";
					}
					if (reason.contains("Instagram business account or token missing")) {
						return "instagram_business_account_missing";
					}
					if (reason.contains("Meta Graph request failed")) {
						return "graph_request_failed";
					}
					if (reason.contains("Meta access token missing in response")) {
						return "meta_access_token_missing";
					}
				}
			}
			current = current.getCause();
		}
		return "token_exchange_failed";
	}

	private void redirectWithStatus(HttpServletResponse response, String status, String reason) throws java.io.IOException {
		redirectToSettings(response, null, status, reason);
	}

	private void redirectWithStatus(HttpServletResponse response, UserAuthToken token, String status, String reason) throws java.io.IOException {
		redirectToSettings(response, token != null ? token.getOauthSourcePage() : null, status, reason);
	}

	private void redirectToSettings(HttpServletResponse response, String sourcePage, String status, String reason) throws java.io.IOException {
		String redirectUrl = publicUrlProperties.resolveFrontendSettingsUrl(sourcePage)
				+ "?socialConnect=" + status
				+ "&platform=" + "instagram";
		if (reason != null && !reason.isBlank()) {
			redirectUrl += "&reason=" + reason.toLowerCase(Locale.ROOT);
		}
		response.sendRedirect(redirectUrl);
	}

	@PostMapping("/post")
	public ResponseEntity<?> createPost(@RequestHeader(value = "x-trace-id", required = true) String xTraceId,
										@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody  TweetRequest request) {
		try {
//			request.setFile(file);

//			TweetResponse response = facebookConnectionService.createInstagramPost(request,userDetails);
			invokePost(request,userDetails);
			TweetResponse response = new TweetResponse();
			response.setStatus(true);
			response.setMessage("Posting process initiated and will be shared to external platform.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error | tweet-with-media | traceId={} | message={}", xTraceId, e.getMessage(), e);
			return ResponseEntity.status(500).body("Error: " + e.getMessage());
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

	private CompletableFuture<Void> invokePost(TweetRequest request, UserDetailsImpl userDetails){
		return CompletableFuture.runAsync(() -> {
			try{
				TweetResponse response = facebookConnectionService.createInstagramPost(request,userDetails);
			}catch (Exception e){
				log.error("Error processing device action: {}",e.getMessage());
			}
		});
	}
}
