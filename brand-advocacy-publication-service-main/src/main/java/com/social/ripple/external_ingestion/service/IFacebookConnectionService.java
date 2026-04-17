package com.social.ripple.external_ingestion.service;

import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import jakarta.servlet.http.HttpSession;

public interface IFacebookConnectionService {

	UserAuthToken handleFbCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId);

	UserAuthToken saveToken(UserAuthToken token);

	UserAuthToken handleInstagramCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId);

	TweetResponse createFbPost(TweetRequest request, UserDetailsImpl userDetails) throws Exception;

	TweetResponse createInstagramPost(TweetRequest request, UserDetailsImpl userDetails) throws Exception;

}
