package com.social.ripple.usermanagement.service.implementation;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.social.ripple.usermanagement.config.PublicUrlProperties;
import com.social.ripple.usermanagement.dao.model.UserAuthToken;
import com.social.ripple.usermanagement.dao.repository.UserAuthTokenRepository;
import com.social.ripple.usermanagement.dto.request.ToggleConnectionRequest;
import com.social.ripple.usermanagement.dto.response.ToggleConnectionResponse;
import com.social.ripple.usermanagement.service.IConnectionService;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ResponseCode;
import com.social.ripple.usermanagement.util.enumeration.Platform;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConnectionServiceImpl implements IConnectionService {
    private static final String ADMIN_SETTINGS = "ADMIN_SETTINGS";
    private static final String EMPLOYEE_SETTINGS = "EMPLOYEE_SETTINGS";

    private final UserAuthTokenRepository userAuthTokenRepository;
    private final com.social.ripple.usermanagement.dao.repository.UserRepository userRepository;
    private final AppCache appCache;
    private final PublicUrlProperties publicUrlProperties;
    private final String metaAppId;
    private final String linkedInClientId;
    private final String linkedInOrgClientId;

    public ConnectionServiceImpl(UserAuthTokenRepository userAuthTokenRepository,
                                 com.social.ripple.usermanagement.dao.repository.UserRepository userRepository,
                                 AppCache appCache,
                                 PublicUrlProperties publicUrlProperties,
                                 @Value("${app.oauth.meta-app-id:1910917019454230}") String metaAppId,
                                 @Value("${app.oauth.linkedin-client-id:}") String linkedInClientId,
                                 @Value("${app.oauth.linkedin-org-client-id:}") String linkedInOrgClientId) {
        this.userAuthTokenRepository = userAuthTokenRepository;
        this.userRepository = userRepository;
        this.appCache = appCache;
        this.publicUrlProperties = publicUrlProperties;
        this.metaAppId = metaAppId;
        this.linkedInClientId = linkedInClientId;
        this.linkedInOrgClientId = linkedInOrgClientId;
    }

    @Override
    @Transactional
    public ToggleConnectionResponse toggleConnection(Long userId, ToggleConnectionRequest request, String traceId) {
        ToggleConnectionResponse response = new ToggleConnectionResponse();
        String platformStr = request.getPlatform(); // now sending platform name as string
        boolean connect = request.isConnect();

        Platform platform;
        try {
            platform = Platform.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            response.setPlatform("UNKNOWN");
            response.setConnected(false);
            response.setMessage("Platform not supported");
            response.setCode(ResponseCode.USMG_404);
            return response;
        }

        response.setPlatform(platform.name());

        Optional<UserAuthToken> tokenOpt = userAuthTokenRepository.findByUserIdAndPlatform(userId, platform);

        if (connect) {
            handleConnect(traceId, userId, platform, request.getSourcePage(), tokenOpt, response);
        } else {
            handleDisconnect(traceId, userId, platform, tokenOpt, response);
        }

        return response;
    }

    private void handleConnect(String traceId, Long userId, Platform platform, String sourcePage,
                               Optional<UserAuthToken> tokenOpt,
                               ToggleConnectionResponse response) {

        if (tokenOpt.isPresent() && Boolean.TRUE.equals(tokenOpt.get().getIsConnected())) {
            response.setConnected(true);
            response.setMessage(platform.name() + " already connected");
            response.setCode(ResponseCode.USMG_200);
            response.setStatus(true);
            return;
        }

        String transactionId = generateTransactionId(userId, platform.name());
        UserAuthToken token = tokenOpt.orElseGet(() -> {
            UserAuthToken t = new UserAuthToken();
            t.setUserId(userId);
            t.setPlatform(platform);
            t.setIsConnected(false);
            t.setStatus("NEVER_CONNECTED");
            t.setCreatedAt(LocalDateTime.now());
            log.info("[{}] | Creating new token | userId={} platform={}", traceId, userId, platform.name());
            return t;
        });

        token.setTransactionId(transactionId);
        token.setTransactionCreatedAt(LocalDateTime.now());
        token.setOauthSourcePage(resolveOauthSourcePage(sourcePage));
        token.setStatus("PENDING_CONNECTION");
        token.setUpdatedAt(LocalDateTime.now());
        userAuthTokenRepository.save(token);

        String authUrl = null;

        if(platform == Platform.X){
            authUrl = appCache.getConfigParameterValue(traceId, ConfigKeys.X_OAUTH_URL);
        }else if(platform == Platform.LINKEDIN){
            boolean isLeader = userRepository.findById(userId)
                    .map(u -> Boolean.TRUE.equals(u.getIsLeader()))
                    .orElse(false);
            String effectiveClientId;
            String scope;
            if (isLeader && linkedInOrgClientId != null && !linkedInOrgClientId.isBlank()) {
                effectiveClientId = linkedInOrgClientId;
                scope = "w_member_social%20r_organization_social%20w_organization_social%20rw_organization_admin";
                token.setClientId("LEADER_LINKEDIN");
                userAuthTokenRepository.save(token);
                log.info("[{}] | LinkedIn leader connection — using App 2 (org) credentials", traceId);
            } else {
                effectiveClientId = linkedInClientId;
                scope = "openid%20profile%20email%20w_member_social";
            }
            if (effectiveClientId == null || effectiveClientId.isBlank()) {
                log.error("[{}] | LinkedIn client ID is not configured", traceId);
            } else {
            authUrl = "https://www.linkedin.com/oauth/v2/authorization?" +
                    "response_type=code" +
                    "&client_id=" + URLEncoder.encode(effectiveClientId, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(publicUrlProperties.getLinkedInCallbackUrl(), StandardCharsets.UTF_8) +
                    "&scope=" + scope;
            }
        }else if(platform == Platform.FACEBOOK){
            authUrl = "https://www.facebook.com/v19.0/dialog/oauth?" +
                    "client_id=" + metaAppId +
                    "&redirect_uri=" + publicUrlProperties.getFacebookCallbackUrl() +
                    "&scope=pages_show_list,pages_read_engagement,pages_manage_posts,pages_manage_metadata,pages_read_user_content&response_type=code";
        }else if(platform == Platform.INSTAGRAM){
            authUrl = "https://www.facebook.com/v19.0/dialog/oauth?" +
                    "client_id=" + metaAppId +
                    "&redirect_uri=" + publicUrlProperties.getInstagramCallbackUrl() +
                    "&scope=instagram_basic,instagram_manage_insights,instagram_content_publish,pages_show_list,pages_manage_posts,pages_manage_metadata,pages_read_engagement" +
                    "&response_type=code";
        }


        if (authUrl == null || authUrl.isEmpty()) {
            response.setConnected(false);
            response.setMessage("OAuth URL not configured for " + platform.name());
            response.setCode(ResponseCode.USMG_500);
            response.setStatus(false);
        } else {
            response.setConnected(false);
            response.setMessage("Redirect user to OAuth URL");

            if(platform == Platform.X) {
                response.setAuthUrl(authUrl + "?transactionId=" + transactionId);
            }else if(platform == Platform.LINKEDIN){
                response.setAuthUrl(authUrl + "&state=" + transactionId);
            }else if(platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM){
                response.setAuthUrl(authUrl + "&state=" + transactionId);
            }
            response.setCode(ResponseCode.USMG_200);
            response.setStatus(true);
        }
    }

    private void handleDisconnect(String traceId, Long userId, Platform platform, Optional<UserAuthToken> tokenOpt,
                                  ToggleConnectionResponse response) {

        if (tokenOpt.isPresent()) {
            UserAuthToken token = tokenOpt.get();
            token.setAccessToken(null);
            token.setAccessSecret(null);
            token.setRefreshToken(null);
            token.setIsConnected(false);
            token.setStatus("DISCONNECTED");
            token.setUpdatedAt(LocalDateTime.now());
            userAuthTokenRepository.save(token);

            response.setConnected(false);
            response.setMessage(platform.name() + " disconnected successfully");
            response.setCode(ResponseCode.USMG_200);
            response.setStatus(true);
        } else {
            response.setConnected(false);
            response.setMessage(platform.name() + " not connected yet");
            response.setCode(ResponseCode.USMG_404);
            response.setStatus(false);
        }
    }

    private String generateTransactionId(Long userId, String platformName) {
        String payload = userId + ":" + platformName + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveOauthSourcePage(String sourcePage) {
        if (ADMIN_SETTINGS.equalsIgnoreCase(sourcePage)) {
            return ADMIN_SETTINGS;
        }
        return EMPLOYEE_SETTINGS;
    }
}
