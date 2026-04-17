package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.dao.model.User;
import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.DashboardResponse;
import com.social.ripple.external_ingestion.dto.response.ImpressionsResponse;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import jakarta.servlet.http.HttpSession;

public interface XConnectionService {

	/**
	 * Build the OAuth authorization URL for the given token.
	 *
	 * @param callbackUrl The callback URL where the OAuth provider will redirect.
	 * @param token       The UserAuthToken containing transactionId.
	 * @param session
	 * @return OAuth authorization URL
	 */
	String getAuthorizationUrl(String callbackUrl, UserAuthToken token, HttpSession session);
	OrgAuthorizationStart beginOrganizationAuthorization(String callbackUrl, HttpSession session);

	/**
	 * Handle the OAuth callback, exchange verifier for tokens, and save.
	 *
	 * @param oauthVerifier OAuth verifier from callback
	 * @param token         The UserAuthToken containing transactionId
	 * @param session
	 * @param transactionId
	 * @return Updated UserAuthToken with accessToken and accessSecret
	 */
	UserAuthToken handleCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId);
	UserAuthToken handleLinkedInCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId);
	OrgAccountConnection exchangeOrganizationAccount(String oauthVerifier, String requestToken, String requestTokenSecret, HttpSession session);

	/**
	 * Save or update the user tokens.
	 *
	 * @param externalUserId External user ID from provider
	 * @param accessToken    Access token
	 * @param accessSecret   Access secret
	 * @param oauthVerifier
	 * @return Updated UserAuthToken
	 */
	UserAuthToken saveUserTokens(String externalUserId, String accessToken, String accessSecret, String oauthVerifier, Platform platform);

	UserAuthToken saveToken(UserAuthToken token);

	UserAuthToken getTokenByTransactionId(String transactionId);

	UserAuthToken getPendingTokenByRequestToken(String requestToken);

	TweetResponse postTweetWithUploadedFile(TweetRequest request, UserDetailsImpl userDetails) throws Exception;

	TweetResponse postTweet(TweetRequest request, UserDetailsImpl userDetails) throws Exception;

	TweetResponse createLinkedinPost(TweetRequest request, UserDetailsImpl userDetails, User user) throws Exception;

    ImpressionsResponse getImpressionsByPostId(Long postId);

    DashboardResponse fetchOrganizationMonthlyReach(String traceId, String tenantId, UserDetailsImpl userDetails);

	DashboardResponse fetchOrganizationLeadConversion(String traceId, String tenantId, UserDetailsImpl userDetails);

	record OrgAccountConnection(
			String externalUserId,
			String accessToken,
			String accessSecret,
			String username,
			String displayName,
			String pageUrl
	) {}

	record OrgAuthorizationStart(
			String authorizationUrl,
			String requestToken,
			String requestTokenSecret
	) {}
}
