package com.social.ripple.external_ingestion.service;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.external_ingestion.constants.ApplicationConstants;
import com.social.ripple.external_ingestion.dao.model.ExternalPlatform;
import com.social.ripple.external_ingestion.dao.model.OrgBusinessConnectTransaction;
import com.social.ripple.external_ingestion.dao.model.Organization;
import com.social.ripple.external_ingestion.dao.model.User;
import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dao.repository.ExternalPlatformRepository;
import com.social.ripple.external_ingestion.dao.repository.OrgBusinessConnectTransactionRepository;
import com.social.ripple.external_ingestion.dao.repository.OrganizationRepository;
import com.social.ripple.external_ingestion.dao.repository.UserAuthTokenRepository;
import com.social.ripple.external_ingestion.dao.repository.UserRepository;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.dto.request.BusinessPageSelectionRequest;
import com.social.ripple.external_ingestion.dto.request.OrgBusinessPageRequest;
import com.social.ripple.external_ingestion.dto.response.BusinessPageOptionDto;
import com.social.ripple.external_ingestion.dto.response.LeaderPageStatusDto;
import com.social.ripple.external_ingestion.dto.response.LeaderPlatformStatusDto;
import com.social.ripple.external_ingestion.dto.response.OrgBusinessPageDto;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.util.enumeration.AuthStatus;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationPlatformSettingsService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ORG_BUSINESS_CLIENT_ID = "ORG_BUSINESS";
    private static final long TRANSACTION_TTL_MINUTES = 10;
    private static final String SYNC_STATUS_NEVER_SYNCED = "NEVER_SYNCED";
    private static final String SYNC_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String SYNC_STATUS_SUCCESS = "SUCCESS";
    private static final String SYNC_STATUS_SUCCESS_WITH_NEW_CONTENT = "SUCCESS_WITH_NEW_CONTENT";
    private static final String SYNC_STATUS_SUCCESS_NO_NEW_CONTENT = "SUCCESS_NO_NEW_CONTENT";
    private static final String SYNC_STATUS_FAILED = "FAILED";
    private static final String META_PROVIDER_LABEL = "Meta";
    private static final String LINKEDIN_PROVIDER_LABEL = "LinkedIn";
    private static final String X_PROVIDER_LABEL = "X";
    private static final String TX_STATUS_PENDING = "PENDING";
    private static final String TX_STATUS_READY = "READY";
    private static final String TX_STATUS_COMPLETED = "COMPLETED";
    private static final Map<Platform, String> LEGACY_PLATFORM_KEYS = Map.of(
            Platform.FACEBOOK, "facebook",
            Platform.INSTAGRAM, "instagram",
            Platform.LINKEDIN, "linkedin",
            Platform.X, "x"
    );

    private final ExternalPlatformRepository externalPlatformRepository;
    private final OrgBusinessConnectTransactionRepository orgBusinessConnectTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserAuthTokenRepository userAuthTokenRepository;
    private final MetaOAuthService metaOAuthService;
    private final LinkedInOrganizationOAuthService linkedInOrganizationOAuthService;
    private final PublicUrlProperties publicUrlProperties;
    private final XConnectionService xConnectionService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    public void bootstrapLegacyBusinessPages() {
        migrateLegacyBusinessPages();
    }

    public List<OrgBusinessPageDto> getBusinessPages(UserDetailsImpl userDetails) {
        ensureOrgAdmin(userDetails);
        Long orgId = userDetails.getOrganization().getId();
        migrateLegacyBusinessPagesForOrganization(orgId);
        Map<Platform, ExternalPlatformPayload> payloads = new LinkedHashMap<>();
        for (Platform platform : Platform.values()) {
            payloads.put(platform, loadOrgPayload(orgId, platform).orElseGet(() -> ExternalPlatformPayload.empty(platform)));
        }
        return payloads.values().stream().map(this::toBusinessPageDto).toList();
    }

    public OrgBusinessPageDto saveBusinessPage(UserDetailsImpl userDetails, Platform platform, OrgBusinessPageRequest request) {
        ensureOrgAdmin(userDetails);
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Manual business page configuration is not supported. Use Connect to link an organization business page."
        );
    }

    public OrgBusinessPageDto saveBusinessPageManual(UserDetailsImpl userDetails, Platform platform, OrgBusinessPageRequest request) {
        ensureOrgAdmin(userDetails);
        if (platform != Platform.LINKEDIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    platform.getDisplayName() + " manual business page configuration is not supported. Use Connect to link an organization business page."
            );
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LinkedIn organization ID and access token are required");
        }

        Long organizationId = userDetails.getOrganization().getId();
        ExternalPlatformPayload existingPayload = loadOrgPayload(organizationId, platform)
                .orElseGet(() -> ExternalPlatformPayload.empty(platform));
        NormalizedRequest normalizedRequest = normalizeRequest(existingPayload, platform, request);
        validateLinkedInManualRequest(normalizedRequest);
        OrgBusinessPageDto page = saveOrganizationPlatform(organizationId, platform, normalizedRequest);
        requestImmediateBusinessPageFetch(organizationId, platform);
        return page;
    }

    public void deleteBusinessPage(UserDetailsImpl userDetails, Platform platform) {
        ensureOrgAdmin(userDetails);
        Long orgId = userDetails.getOrganization().getId();
        externalPlatformRepository.findByOrganizationIdAndPlatformNameIgnoreCase(orgId, platform.getDisplayName())
                .ifPresent(externalPlatformRepository::delete);
        pruneBusinessPageConfig(orgId);
    }

    public String startMetaBusinessConnect(UserDetailsImpl userDetails, Platform platform) {
        ensureOrgAdmin(userDetails);
        ensureMetaPlatform(platform);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        UserAuthToken token = userAuthTokenRepository.findByUserIdAndPlatform(userDetails.getUserId(), platform)
                .orElseGet(UserAuthToken::new);

        if (token.getId() == null) {
            token.setUserId(userDetails.getUserId());
            token.setPlatform(platform);
            token.setStatus(AuthStatus.PENDING_CONNECTION);
            token.setIsConnected(false);
            token.setCreatedAt(now);
        }

        token.setClientId(ORG_BUSINESS_CLIENT_ID);
        token.setOauthSourcePage("BUSINESS");
        token.setTransactionStatus(TransactionStatus.ACTIVE);
        token.setTransactionCreatedAt(now);
        token.setTransactionId(generateTransactionId(userDetails.getOrganization().getId(), platform));
        token.setUpdatedAt(now);
        userAuthTokenRepository.save(token);
        return metaOAuthService.buildAuthorizationUrl(platform, orgCallbackUrl(platform), token.getTransactionId());
    }

    public String startBusinessConnect(UserDetailsImpl userDetails, Platform platform, HttpSession session) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM -> startMetaBusinessConnect(userDetails, platform);
            case LINKEDIN -> startLinkedInBusinessConnect(userDetails);
            case X -> startXBusinessConnect(userDetails, session);
        };
    }

    public boolean completeBusinessConnect(Platform platform,
                                           String code,
                                           String transactionId,
                                           String oauthToken,
                                           String oauthVerifier,
                                           String xTransactionId,
                                           HttpSession session) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM -> {
                completeMetaBusinessConnect(code, transactionId, platform);
                yield false;
            }
            case LINKEDIN -> {
                completeLinkedInBusinessConnect(code, transactionId);
                yield true; // show selection UI
            }
            case X -> {
                completeXBusinessConnect(oauthVerifier, oauthToken, xTransactionId, session);
                yield false;
            }
        };
    }

    public boolean hasActiveBusinessConnectTransaction(String transactionId, Platform platform) {
        return orgBusinessConnectTransactionRepository.findByTransactionId(transactionId)
                .filter(transaction -> transaction.getPlatform() == platform)
                .isPresent();
    }

    public String resolveActiveBusinessConnectTransactionIdByRequestToken(String requestToken, Platform platform) {
        if (!StringUtils.hasText(requestToken)) {
            return null;
        }
        return orgBusinessConnectTransactionRepository.findByTemporaryAccessToken(requestToken)
                .filter(transaction -> transaction.getPlatform() == platform)
                .filter(transaction -> transaction.getExpiresAt() == null || !transaction.getExpiresAt().isBefore(LocalDateTime.now()))
                .map(OrgBusinessConnectTransaction::getTransactionId)
                .orElse(null);
    }

    public void completeSharedXBusinessConnect(String oauthVerifier, String oauthToken, String transactionId, HttpSession session) {
        completeXBusinessConnect(oauthVerifier, oauthToken, transactionId, session);
    }

    public OrgBusinessPageDto completeMetaBusinessConnect(String code, String transactionId, Platform platform) {
        ensureMetaPlatform(platform);
        UserAuthToken token = userAuthTokenRepository.findByTransactionIdAndPlatform(transactionId, platform)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid transactionId"));
        if (!ORG_BUSINESS_CLIENT_ID.equals(token.getClientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not an organization business-page flow");
        }
        if (isTransactionExpired(token.getTransactionCreatedAt())) {
            clearMetaBusinessConnectTransaction(token);
            throw new ResponseStatusException(HttpStatus.GONE, "Business page connection link has expired");
        }

        Long organizationId = resolveOrganizationId(token);

        ExternalPlatformPayload existingPayload = loadOrgPayload(organizationId, platform)
                .orElseGet(() -> ExternalPlatformPayload.empty(platform));

        MetaOAuthService.MetaBusinessConnection connection = metaOAuthService.exchangeBusinessConnection(
                platform,
                code,
                orgCallbackUrl(platform),
                existingPayload.externalUserId,
                existingPayload.pageId
        );

        OrgBusinessPageRequest request = new OrgBusinessPageRequest();
        request.setEnabled(Boolean.TRUE);
        request.setApiUrl(defaultApiUrl(platform));
        request.setAccessToken(connection.getAccessToken());
        request.setExternalUserId(connection.getExternalUserId());
        request.setPageId(connection.getPageId());
        request.setUsername(connection.getUsername());
        request.setPageUrl(connection.getPageUrl());
        request.setDisplayName(connection.getDisplayName());

        ExternalPlatform record = externalPlatformRepository
                .findByOrganizationIdAndPlatformNameIgnoreCase(organizationId, platform.getDisplayName())
                .orElseGet(ExternalPlatform::new);
        record.setOrganizationId(organizationId);
        record.setPlatformName(platform.getDisplayName());
        record.setApiUrl(defaultApiUrl(platform));
        record.setEnabled(Boolean.TRUE);
        NormalizedRequest normalizedRequest = normalizeRequest(existingPayload, platform, request);
        validateRequest(platform, normalizedRequest);
        record.setCredentials(writePayload(normalizedRequest));
        ExternalPlatform saved = externalPlatformRepository.save(record);

        pruneBusinessPageConfig(organizationId);
        clearMetaBusinessConnectTransaction(token);
        requestImmediateBusinessPageFetch(organizationId, platform);
        return toBusinessPageDto(ExternalPlatformPayload.from(saved, platform));
    }

    private void clearMetaBusinessConnectTransaction(UserAuthToken token) {
        token.setClientId(null);
        token.setTransactionId(null);
        token.setTransactionCreatedAt(null);
        token.setTransactionStatus(null);
        token.setUpdatedAt(java.time.LocalDateTime.now());
        token.setStatus(Boolean.TRUE.equals(token.getIsConnected()) ? AuthStatus.CONNECTED : AuthStatus.NEVER_CONNECTED);
        userAuthTokenRepository.save(token);
    }

    private void completeXBusinessConnect(String oauthVerifier, String oauthToken, String transactionId, HttpSession session) {
        OrgBusinessConnectTransaction transaction = resolveActiveXTransaction(transactionId, oauthToken);
        if (StringUtils.hasText(oauthToken) && !oauthToken.equals(transaction.getTemporaryAccessToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X business page callback token mismatch");
        }
        XConnectionService.OrgAccountConnection connection = xConnectionService.exchangeOrganizationAccount(
                oauthVerifier,
                transaction.getTemporaryAccessToken(),
                transaction.getTemporaryAccessSecret(),
                session
        );

        // X only returns one handle — auto-save directly without selection step
        NormalizedRequest normalizedRequest = new NormalizedRequest(
                true,
                defaultApiUrl(Platform.X),
                connection.accessToken(),
                connection.accessSecret(),
                connection.externalUserId(),
                null,
                connection.username(),
                connection.pageUrl(),
                connection.pageUrl(),
                connection.displayName()
        );
        validateRequest(Platform.X, normalizedRequest);
        saveOrganizationPlatform(transaction.getOrganizationId(), Platform.X, normalizedRequest);
        transaction.setStatus(TX_STATUS_COMPLETED);
        orgBusinessConnectTransactionRepository.delete(transaction);
        requestImmediateBusinessPageFetch(transaction.getOrganizationId(), Platform.X);
    }

    public List<BusinessPageOptionDto> getAvailableBusinessPages(UserDetailsImpl userDetails, Platform platform, String transactionId) {
        ensureOrgAdmin(userDetails);
        OrgBusinessConnectTransaction transaction = getActiveTransaction(transactionId, platform);
        if (!Objects.equals(transaction.getOrganizationId(), userDetails.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Business page transaction does not belong to this organization");
        }
        return readAvailablePages(transaction);
    }

    public OrgBusinessPageDto selectBusinessPage(UserDetailsImpl userDetails, Platform platform, BusinessPageSelectionRequest request) {
        ensureOrgAdmin(userDetails);
        if (request == null || !StringUtils.hasText(request.getTransactionId()) || !StringUtils.hasText(request.getSelectionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction id and selection id are required");
        }
        OrgBusinessConnectTransaction transaction = getActiveTransaction(request.getTransactionId(), platform);
        if (!Objects.equals(transaction.getOrganizationId(), userDetails.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Business page transaction does not belong to this organization");
        }
        BusinessPageOptionDto selectedOption = readAvailablePages(transaction).stream()
                .filter(option -> request.getSelectionId().equals(option.getSelectionId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected business page was not found"));

        NormalizedRequest normalizedRequest = switch (platform) {
            case LINKEDIN -> new NormalizedRequest(
                    true,
                    defaultApiUrl(platform),
                    transaction.getTemporaryAccessToken(),
                    transaction.getTemporaryRefreshToken(),
                    selectedOption.getExternalUserId(),
                    selectedOption.getPageId(),
                    selectedOption.getUsername(),
                    selectedOption.getPageUrl(),
                    firstNonBlank(selectedOption.getBusinessPageLink(), selectedOption.getPageUrl()),
                    selectedOption.getDisplayName()
            );
            case X -> new NormalizedRequest(
                    true,
                    defaultApiUrl(platform),
                    transaction.getTemporaryAccessToken(),
                    transaction.getTemporaryAccessSecret(),
                    selectedOption.getExternalUserId(),
                    selectedOption.getPageId(),
                    selectedOption.getUsername(),
                    selectedOption.getPageUrl(),
                    firstNonBlank(selectedOption.getBusinessPageLink(), selectedOption.getPageUrl()),
                    selectedOption.getDisplayName()
            );
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selection flow is not used for this platform");
        };
        validateRequest(platform, normalizedRequest);
        OrgBusinessPageDto page = saveOrganizationPlatform(transaction.getOrganizationId(), platform, normalizedRequest);
        transaction.setStatus(TX_STATUS_COMPLETED);
        orgBusinessConnectTransactionRepository.delete(transaction);
        requestImmediateBusinessPageFetch(transaction.getOrganizationId(), platform);
        return page;
    }

    public List<LeaderPageStatusDto> getLeaderPages(UserDetailsImpl userDetails) {
        ensureOrgAdmin(userDetails);
        Long orgId = userDetails.getOrganization().getId();
        List<User> leaders = userRepository.findByOrganizationIdAndIsLeaderTrue(orgId);
        if (leaders.isEmpty()) {
            return List.of();
        }

        Map<Long, List<UserAuthToken>> tokenMap = new LinkedHashMap<>();
        userAuthTokenRepository.findByUserIdIn(leaders.stream().map(User::getId).toList())
                .forEach(token -> tokenMap.computeIfAbsent(token.getUserId(), ignored -> new ArrayList<>()).add(token));

        return leaders.stream()
                .map(leader -> LeaderPageStatusDto.builder()
                        .userId(leader.getId())
                        .name(leader.getName())
                        .email(leader.getEmail())
                        .leader(Boolean.TRUE.equals(leader.getIsLeader()))
                        .platforms(buildLeaderPlatforms(tokenMap.getOrDefault(leader.getId(), List.of())))
                        .build())
                .toList();
    }

    private OrgBusinessConnectTransaction createTransaction(UserDetailsImpl userDetails, Platform platform) {
        OrgBusinessConnectTransaction transaction = new OrgBusinessConnectTransaction();
        transaction.setTransactionId(generateTransactionId(userDetails.getOrganization().getId(), platform));
        transaction.setOrganizationId(userDetails.getOrganization().getId());
        transaction.setInitiatedByUserId(userDetails.getUserId());
        transaction.setPlatform(platform);
        transaction.setStatus(TX_STATUS_PENDING);
        transaction.setExpiresAt(LocalDateTime.now().plusMinutes(TRANSACTION_TTL_MINUTES));
        return transaction;
    }

    private OrgBusinessConnectTransaction getActiveTransaction(String transactionId, Platform platform) {
        OrgBusinessConnectTransaction transaction = orgBusinessConnectTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business page transaction not found"));
        if (transaction.getPlatform() != platform) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business page transaction platform mismatch");
        }
        if (transaction.getExpiresAt() != null && transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            orgBusinessConnectTransactionRepository.delete(transaction);
            throw new ResponseStatusException(HttpStatus.GONE, "Business page connection has expired");
        }
        return transaction;
    }

    private OrgBusinessConnectTransaction resolveActiveXTransaction(String transactionId, String oauthToken) {
        if (StringUtils.hasText(transactionId)) {
            return getActiveTransaction(transactionId, Platform.X);
        }
        if (!StringUtils.hasText(oauthToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X business page callback parameters");
        }
        OrgBusinessConnectTransaction transaction = orgBusinessConnectTransactionRepository.findByTemporaryAccessToken(oauthToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business page transaction not found"));
        if (transaction.getPlatform() != Platform.X) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business page transaction platform mismatch");
        }
        if (transaction.getExpiresAt() != null && transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            orgBusinessConnectTransactionRepository.delete(transaction);
            throw new ResponseStatusException(HttpStatus.GONE, "Business page connection has expired");
        }
        return transaction;
    }

    private List<BusinessPageOptionDto> readAvailablePages(OrgBusinessConnectTransaction transaction) {
        if (!StringUtils.hasText(transaction.getDiscoveredPagesJson())) {
            return List.of();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(transaction.getDiscoveredPagesJson());
            List<BusinessPageOptionDto> pages = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    pages.add(BusinessPageOptionDto.builder()
                            .selectionId(text(node, "selectionId"))
                            .platform(firstNonBlank(text(node, "platform"), transaction.getPlatform().name()))
                            .externalUserId(text(node, "externalUserId"))
                            .pageId(text(node, "pageId"))
                            .username(text(node, "username"))
                            .pageUrl(text(node, "pageUrl"))
                            .businessPageLink(firstNonBlank(text(node, "businessPageLink"), text(node, "pageUrl")))
                            .displayName(text(node, "displayName"))
                            .build());
                }
            }
            return pages;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to read available business pages", ex);
        }
    }

    private String writeAvailablePages(List<BusinessPageOptionDto> pages) {
        try {
            return OBJECT_MAPPER.writeValueAsString(pages == null ? List.of() : pages);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to store available business pages", ex);
        }
    }

    private OrgBusinessPageDto saveOrganizationPlatform(Long organizationId, Platform platform, NormalizedRequest normalizedRequest) {
        ExternalPlatform record = externalPlatformRepository
                .findByOrganizationIdAndPlatformNameIgnoreCase(organizationId, platform.getDisplayName())
                .orElseGet(ExternalPlatform::new);
        record.setOrganizationId(organizationId);
        record.setPlatformName(platform.getDisplayName());
        record.setApiUrl(normalizedRequest.apiUrl());
        record.setEnabled(normalizedRequest.enabled());
        record.setCredentials(writePayload(normalizedRequest));
        ExternalPlatform saved = externalPlatformRepository.save(record);
        pruneBusinessPageConfig(organizationId);
        return toBusinessPageDto(ExternalPlatformPayload.from(saved, platform));
    }

    public List<ResolvedOrgPlatformConfig> getEnabledPlatformConfigs(Platform platform) {
        List<ResolvedOrgPlatformConfig> configs = resolveAndFilterConfigs(platform);
        if (!configs.isEmpty()) {
            return configs;
        }

        migrateLegacyBusinessPages();

        return resolveAndFilterConfigs(platform);
    }

    private List<ResolvedOrgPlatformConfig> resolveAndFilterConfigs(Platform platform) {
        List<ResolvedOrgPlatformConfig> allConfigs = externalPlatformRepository.findEnabledByPlatformNames(platformNames(platform)).stream()
                .map(record -> ExternalPlatformPayload.from(record, platform))
                .map(payload -> new ResolvedOrgPlatformConfig(payload.organizationId, payload.platform, payload.apiUrl,
                        payload.accessToken, payload.refreshToken, payload.externalUserId, payload.pageId,
                        payload.username, payload.pageUrl, payload.businessPageLink, payload.displayName))
                .toList();
        List<ResolvedOrgPlatformConfig> usable = allConfigs.stream()
                .filter(ResolvedOrgPlatformConfig::isUsable)
                .toList();
        int dropped = allConfigs.size() - usable.size();
        if (dropped > 0) {
            log.warn("{}|getEnabledPlatformConfigs|Dropped {} of {} configs (missing accessToken/token or userId/pageId in credentials JSON). "
                    + "Check external_platforms.credentials for enabled {} rows.",
                    platform, dropped, allConfigs.size(), platform);
        }
        return usable;
    }

    public void markSyncStarted(Long organizationId, Platform platform) {
        updateSyncState(organizationId, platform, record -> {
            record.setLastSyncStatus(SYNC_STATUS_IN_PROGRESS);
            record.setLastSyncAttemptAt(LocalDateTime.now());
        });
    }

    public void markSyncSuccess(Long organizationId, Platform platform, int importedCount) {
        updateSyncState(organizationId, platform, record -> {
            LocalDateTime now = LocalDateTime.now();
            record.setLastSyncStatus(successStatusFor(importedCount));
            record.setLastSyncSuccessAt(now);
            record.setLastSyncError(null);
            record.setLastSyncErrorAt(null);
            record.setLastImportedCount(importedCount);
        });
    }

    public void markSyncSuccess(Long organizationId, Platform platform) {
        markSyncSuccess(organizationId, platform, 1);
    }

    public void markSyncFailure(Long organizationId, Platform platform, String reason) {
        updateSyncState(organizationId, platform, record -> {
            record.setLastSyncStatus(SYNC_STATUS_FAILED);
            record.setLastSyncError(sanitizeSyncError(reason));
            record.setLastSyncErrorAt(LocalDateTime.now());
        });
    }

    public void markLeaderSyncStarted(Long userId, Platform platform) {
        updateLeaderSyncState(userId, platform, token -> {
            token.setLastSyncStatus(SYNC_STATUS_IN_PROGRESS);
            token.setLastSyncAttemptAt(LocalDateTime.now());
        });
    }

    public void markLeaderSyncSuccess(Long userId, Platform platform, int importedCount) {
        updateLeaderSyncState(userId, platform, token -> {
            LocalDateTime now = LocalDateTime.now();
            token.setLastSyncStatus(successStatusFor(importedCount));
            token.setLastSyncSuccessAt(now);
            token.setLastSyncError(null);
            token.setLastSyncErrorAt(null);
        });
    }

    public void markLeaderSyncSuccess(Long userId, Platform platform) {
        markLeaderSyncSuccess(userId, platform, 1);
    }

    public void markLeaderSyncFailure(Long userId, Platform platform, String reason) {
        updateLeaderSyncState(userId, platform, token -> {
            String sanitizedReason = sanitizeSyncError(reason);
            token.setLastSyncStatus(SYNC_STATUS_FAILED);
            token.setLastSyncError(sanitizedReason);
            token.setLastSyncErrorAt(LocalDateTime.now());
            if (shouldDisconnectLeaderToken(sanitizedReason)) {
                token.setIsConnected(false);
                token.setStatus(AuthStatus.DISCONNECTED);
            }
        });
    }

    public String composeBusinessPageContent(Long organizationId, Platform platform, String baseContent) {
        String normalizedBaseContent = baseContent == null ? "" : baseContent.trim();
        if (organizationId == null) {
            return normalizedBaseContent;
        }

        migrateLegacyBusinessPagesForOrganization(organizationId);
        ExternalPlatformPayload payload = loadOrgPayload(organizationId, platform).orElseGet(() -> ExternalPlatformPayload.empty(platform));
        String suffix = payload.contentSuffix();
        if (!StringUtils.hasText(suffix)) {
            return normalizedBaseContent;
        }
        if (!StringUtils.hasText(normalizedBaseContent)) {
            return suffix;
        }
        if (normalizedBaseContent.contains(suffix)) {
            return normalizedBaseContent;
        }
        return normalizedBaseContent + "\n\n" + suffix;
    }

    private void migrateLegacyBusinessPages() {
        organizationRepository.findAll().forEach(organization -> migrateLegacyBusinessPagesForOrganization(organization.getId()));
    }

    private boolean shouldDisconnectLeaderToken(String reason) {
        // Only disconnect for genuine auth revocations, not API-level errors
        // like credits depleted (402), permission scope issues (403), or rate limits
        return false;
    }

    public Optional<User> findOrganizationPostedUser(Long organizationId) {
        return userRepository.findFirstByOrganizationIdOrderByIdAsc(organizationId);
    }

    public List<ResolvedLeaderPlatformConfig> getConnectedLeaderPlatformConfigs(Platform platform) {
        return userAuthTokenRepository.findConnectedLeaderTokensByPlatform(platform).stream()
                .map(token -> userRepository.findById(token.getUserId())
                        .filter(user -> Boolean.TRUE.equals(user.getIsLeader()))
                        .map(user -> ResolvedLeaderPlatformConfig.builder()
                                .userId(user.getId())
                                .organizationId(user.getOrganization().getId())
                                .platform(platform)
                                .apiUrl(defaultApiUrl(platform))
                                .accessToken(token.getAccessToken())
                                .accessSecret(token.getAccessSecret())
                                .refreshToken(token.getRefreshToken())
                                .externalUserId(token.getUserIdExternal())
                                .clientId(token.getClientId())
                                .build()))
                .flatMap(Optional::stream)
                .filter(ResolvedLeaderPlatformConfig::isUsable)
                .toList();
    }

    private List<LeaderPlatformStatusDto> buildLeaderPlatforms(List<UserAuthToken> tokens) {
        Map<Platform, UserAuthToken> tokenByPlatform = new LinkedHashMap<>();
        tokens.forEach(token -> tokenByPlatform.put(token.getPlatform(), token));
        List<LeaderPlatformStatusDto> result = new ArrayList<>();
        for (Platform platform : Platform.values()) {
            UserAuthToken token = tokenByPlatform.get(platform);
            result.add(LeaderPlatformStatusDto.builder()
                    .platform(platform.name())
                    .status(token != null && token.getStatus() != null ? token.getStatus().name() : "NOT_CONNECTED")
                    .externalUserId(token != null ? token.getUserIdExternal() : null)
                    .updatedAt(token != null && token.getUpdatedAt() != null
                            ? token.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                            : null)
                    .lastSyncAttemptAt(token != null ? formatDateTime(token.getLastSyncAttemptAt()) : null)
                    .lastSyncSuccessAt(token != null ? formatDateTime(token.getLastSyncSuccessAt()) : null)
                    .lastSyncStatus(token != null && StringUtils.hasText(token.getLastSyncStatus())
                            ? token.getLastSyncStatus()
                            : SYNC_STATUS_NEVER_SYNCED)
                    .lastSyncError(token != null ? token.getLastSyncError() : null)
                    .lastSyncErrorAt(token != null ? formatDateTime(token.getLastSyncErrorAt()) : null)
                    .build());
        }
        return result;
    }

    private Optional<ExternalPlatformPayload> loadOrgPayload(Long orgId, Platform platform) {
        return findOrgPlatformRecord(orgId, platform)
                .map(record -> ExternalPlatformPayload.from(record, platform));
    }

    private OrgBusinessPageDto toBusinessPageDto(ExternalPlatformPayload payload) {
        return OrgBusinessPageDto.builder()
                .platform(payload.platform.name())
                .connected(payload.connected())
                .enabled(payload.enabled)
                .oauthSupported(isOauthSupported(payload.platform))
                .manualFallbackAvailable(isManualFallbackAvailable(payload.platform))
                .oauthProviderLabel(providerLabel(payload.platform))
                .supportMessage(supportMessage(payload.platform))
                .configSource("EXTERNAL_PLATFORM")
                .apiUrl(payload.apiUrl)
                .accessTokenPreview(maskToken(payload.accessToken))
                .accessTokenConfigured(StringUtils.hasText(payload.accessToken))
                .refreshTokenPreview(maskToken(payload.refreshToken))
                .refreshTokenConfigured(StringUtils.hasText(payload.refreshToken))
                .externalUserId(payload.externalUserId)
                .pageId(payload.pageId)
                .username(payload.username)
                .pageUrl(payload.pageUrl)
                .businessPageLink(payload.businessPageLink)
                .displayName(payload.displayName)
                .lastSyncAttemptAt(formatDateTime(payload.lastSyncAttemptAt))
                .lastSyncSuccessAt(formatDateTime(payload.lastSyncSuccessAt))
                .lastSyncStatus(payload.lastSyncStatus)
                .lastSyncError(payload.lastSyncError)
                .lastSyncErrorAt(formatDateTime(payload.lastSyncErrorAt))
                .lastImportedCount(payload.lastImportedCount)
                .scheduleCadenceLabel(scheduleCadenceLabel(payload.platform))
                .nextScheduledFetchAt(formatDateTime(nextScheduledFetchAt(payload.platform)))
                .build();
    }

    private String scheduleCadenceLabel(Platform platform) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM -> "Every 10 minutes";
            case LINKEDIN, X -> "Every 11 minutes";
        };
    }

    private LocalDateTime nextScheduledFetchAt(Platform platform) {
        int intervalMinutes = switch (platform) {
            case FACEBOOK, INSTAGRAM -> 10;
            case LINKEDIN, X -> 11;
        };

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
        int remainder = currentMinute.getMinute() % intervalMinutes;
        int minutesToAdd = remainder == 0 ? intervalMinutes : intervalMinutes - remainder;
        return currentMinute.plusMinutes(minutesToAdd);
    }

    private boolean isOauthSupported(Platform platform) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM, LINKEDIN, X -> true;
        };
    }

    private boolean isManualFallbackAvailable(Platform platform) {
        return switch (platform) {
            case LINKEDIN -> true;
            case FACEBOOK, INSTAGRAM, X -> false;
        };
    }

    private String writePayload(NormalizedRequest request) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        putIfPresent(root, "accessToken", request.accessToken());
        putIfPresent(root, "refreshToken", request.refreshToken());
        putIfPresent(root, "externalUserId", request.externalUserId());
        putIfPresent(root, "pageId", request.pageId());
        putIfPresent(root, "username", request.username());
        putIfPresent(root, "pageUrl", request.pageUrl());
        putIfPresent(root, "businessPageLink", request.businessPageLink());
        putIfPresent(root, "displayName", request.displayName());
        return root.toString();
    }

    private NormalizedRequest normalizeRequest(ExternalPlatformPayload existingPayload, Platform platform, OrgBusinessPageRequest request) {
        return new NormalizedRequest(
                request.getEnabled() == null ? Boolean.TRUE : request.getEnabled(),
                StringUtils.hasText(request.getApiUrl()) ? request.getApiUrl().trim() : defaultApiUrl(platform),
                mergeSecret(request.getAccessToken(), existingPayload.accessToken),
                mergeSecret(request.getRefreshToken(), existingPayload.refreshToken),
                normalize(request.getExternalUserId()),
                normalize(request.getPageId()),
                normalize(request.getUsername()),
                firstNonBlank(normalize(request.getPageUrl()), existingPayload.pageUrl, defaultPageUrl(platform,
                        normalize(request.getUsername()), normalize(request.getPageId()), normalize(request.getExternalUserId()))),
                firstNonBlank(normalize(request.getBusinessPageLink()), normalize(request.getPageUrl()),
                        existingPayload.businessPageLink, existingPayload.pageUrl,
                        defaultPageUrl(platform, normalize(request.getUsername()), normalize(request.getPageId()),
                                normalize(request.getExternalUserId()))),
                normalize(request.getDisplayName())
        );
    }

    private void validateRequest(Platform platform, NormalizedRequest request) {
        if (!request.enabled()) {
            return;
        }
        if (!StringUtils.hasText(request.accessToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, platform.getDisplayName() + " access token is required");
        }
        if (!StringUtils.hasText(request.externalUserId())
                && !StringUtils.hasText(request.pageId())
                && !StringUtils.hasText(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one page/account identifier is required for " + platform.getDisplayName()
            );
        }
    }

    private void validateLinkedInManualRequest(NormalizedRequest request) {
        validateRequest(Platform.LINKEDIN, request);

        if (!StringUtils.hasText(request.externalUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LinkedIn organization ID is required");
        }

        String externalUserId = request.externalUserId().trim();
        String normalizedLower = externalUserId.toLowerCase(Locale.ROOT);
        if (normalizedLower.startsWith("urn:li:person:")
                || normalizedLower.contains("/in/")
                || normalizedLower.contains("linkedin.com/in/")
                || normalizedLower.startsWith("person:")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "LinkedIn manual fallback accepts only organization/company identifiers. Personal LinkedIn profiles are not allowed."
            );
        }

        if (normalizedLower.startsWith("urn:li:organization:")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter the LinkedIn organization ID only, not the full organization URN."
            );
        }
    }

    private String mergeSecret(String incoming, String existing) {
        return StringUtils.hasText(incoming) ? incoming.trim() : existing;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return "********";
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    private String providerLabel(Platform platform) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM -> META_PROVIDER_LABEL;
            case LINKEDIN -> LINKEDIN_PROVIDER_LABEL;
            case X -> X_PROVIDER_LABEL;
        };
    }

    private String supportMessage(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> "Connect only organization-owned Facebook Pages through the shared Meta app. Personal Facebook profiles are not allowed.";
            case INSTAGRAM -> "Connect only Instagram business or professional accounts linked to a Facebook Page. Personal Instagram accounts are not allowed.";
            case LINKEDIN -> "Connect your organization's LinkedIn company page. You must be an admin of the LinkedIn company page to connect it. Manual setup is also available as a fallback.";
            case X -> "Connect the X account used as your organization brand handle, then confirm it in settings. X does not expose a separate business-page discovery model here, so the organization admin must verify the connected handle is organization-owned.";
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z" : null;
    }

    private String sanitizeSyncError(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String normalized = reason.replaceAll("\\s+", " ").trim();
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("unauthorized") || lowerCase.contains(" 401") || lowerCase.contains("(401)")) {
            return "Unauthorized (401)";
        }
        if (lowerCase.contains("rate limit") || lowerCase.contains("too many requests") || lowerCase.contains(" 429")
                || lowerCase.contains("(429)")) {
            return "Rate limited (429)";
        }
        if (lowerCase.contains("incorrect string value")
                || lowerCase.contains("jpasystemexception")
                || lowerCase.contains("could not execute statement")) {
            return "DB encoding error while saving post";
        }
        if (lowerCase.contains(" 404") || lowerCase.contains("(404)") || lowerCase.contains("not found")) {
            return "Not found (404)";
        }
        if (lowerCase.contains("timeout")) {
            return "Connection timeout";
        }

        java.util.regex.Matcher httpStatusMatcher = java.util.regex.Pattern.compile("\\b([45]\\d{2})\\b").matcher(normalized);
        if (httpStatusMatcher.find()) {
            return "HTTP " + httpStatusMatcher.group(1);
        }

        return normalized.length() > 160 ? normalized.substring(0, 160) : normalized;
    }

    private String successStatusFor(int importedCount) {
        return importedCount > 0 ? SYNC_STATUS_SUCCESS_WITH_NEW_CONTENT : SYNC_STATUS_SUCCESS_NO_NEW_CONTENT;
    }

    private void requestImmediateBusinessPageFetch(Long organizationId, Platform platform) {
        if (organizationId == null || platform == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new BusinessPageFetchRequestedEvent(organizationId, platform));
    }

    private void updateSyncState(Long organizationId, Platform platform, java.util.function.Consumer<ExternalPlatform> mutator) {
        findOrgPlatformRecord(organizationId, platform)
                .ifPresent(record -> {
                    mutator.accept(record);
                    externalPlatformRepository.save(record);
                });
    }

    private void updateLeaderSyncState(Long userId, Platform platform, java.util.function.Consumer<UserAuthToken> mutator) {
        userAuthTokenRepository.findByUserIdAndPlatform(userId, platform)
                .ifPresent(token -> {
                    mutator.accept(token);
                    userAuthTokenRepository.save(token);
                });
    }

    private void pruneBusinessPageConfig(Long orgId) {
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        ObjectNode root = parseOrganizationConfig(organization.getSocialHandleConfig());
        removeBusinessPageNodes(root);

        organization.setSocialHandleConfig(root.size() == 0 ? null : root.toString());
        organizationRepository.save(organization);
    }

    private ObjectNode parseOrganizationConfig(String config) {
        if (!StringUtils.hasText(config)) {
            return OBJECT_MAPPER.createObjectNode();
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(config);
            if (node instanceof ObjectNode objectNode) {
                return objectNode.deepCopy();
            }
        } catch (Exception ignored) {
        }
        return OBJECT_MAPPER.createObjectNode();
    }

    private void ensureOrgAdmin(UserDetailsImpl userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(ApplicationConstants.ADMIN_ROLE::equals);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization admin access required");
        }
    }

    private void ensureMetaPlatform(Platform platform) {
        if (!(platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meta connect is supported only for Facebook and Instagram");
        }
    }

    private void migrateLegacyBusinessPagesForOrganization(Long orgId) {
        Organization organization = organizationRepository.findById(orgId).orElse(null);
        if (organization == null) {
            return;
        }

        ObjectNode legacyRoot = parseOrganizationConfig(organization.getSocialHandleConfig());
        boolean updated = false;
        for (Platform platform : Platform.values()) {
            Optional<ExternalPlatform> existingRecord = findOrgPlatformRecord(orgId, platform);
            ExternalPlatformPayload existingPayload = existingRecord
                    .map(record -> ExternalPlatformPayload.from(record, platform))
                    .orElseGet(() -> ExternalPlatformPayload.empty(platform));
            NormalizedRequest legacyRequest = buildLegacyRequest(platform, legacyRoot);
            if (!hasLegacyData(legacyRequest)) {
                continue;
            }
            if (existingRecord.isPresent() && !shouldReplaceWithLegacy(existingPayload)) {
                continue;
            }

            ExternalPlatform record = existingRecord.orElseGet(ExternalPlatform::new);
            record.setOrganizationId(orgId);
            record.setPlatformName(platform.getDisplayName());
            record.setApiUrl(legacyRequest.apiUrl());
            record.setEnabled(legacyRequest.enabled());
            record.setCredentials(writePayload(legacyRequest));
            externalPlatformRepository.save(record);
            updated = true;
        }

        if (updated || containsLegacyBusinessPageNodes(legacyRoot)) {
            removeBusinessPageNodes(legacyRoot);
            organization.setSocialHandleConfig(legacyRoot.size() == 0 ? null : legacyRoot.toString());
            organizationRepository.save(organization);
        }
    }

    private boolean hasLegacyData(NormalizedRequest request) {
        return StringUtils.hasText(request.accessToken())
                || StringUtils.hasText(request.externalUserId())
                || StringUtils.hasText(request.pageId())
                || StringUtils.hasText(request.username())
                || StringUtils.hasText(request.pageUrl())
                || StringUtils.hasText(request.displayName());
    }

    private NormalizedRequest buildLegacyRequest(Platform platform, ObjectNode legacyRoot) {
        ObjectNode platformNode = getLegacyPlatformNode(legacyRoot, platform);
        String accessToken = legacyValue(platformNode, "accessToken", "token", "pageAccessToken");
        String refreshToken = legacyValue(platformNode, "refreshToken", "tokenSecret", "accessSecret");
        String externalUserId = switch (platform) {
            case FACEBOOK -> firstNonBlank(legacyValue(platformNode, "externalUserId", "userId", "pageId", "id"));
            case INSTAGRAM -> firstNonBlank(legacyValue(platformNode, "externalUserId", "userId", "instagramUserId", "instagramBusinessId", "id"));
            case LINKEDIN -> firstNonBlank(legacyValue(platformNode, "externalUserId", "organizationId", "companyId", "userId", "id"));
            case X -> firstNonBlank(legacyValue(platformNode, "externalUserId", "userId", "id"));
        };
        String pageId = switch (platform) {
            case FACEBOOK -> firstNonBlank(legacyValue(platformNode, "pageId", "id"));
            case INSTAGRAM -> legacyValue(platformNode, "pageId");
            case LINKEDIN, X -> null;
        };
        String username = legacyValue(platformNode, "username", "userName", "handle", "screenName");
        String pageUrl = firstNonBlank(legacyValue(platformNode, "pageUrl", "profileUrl", "url", "link"),
                defaultPageUrl(platform, username, pageId, externalUserId));
        String businessPageLink = firstNonBlank(legacyValue(platformNode, "businessPageLink", "business_link"),
                pageUrl, defaultPageUrl(platform, username, pageId, externalUserId));
        String displayName = legacyValue(platformNode, "displayName", "pageName", "name");
        boolean enabled = StringUtils.hasText(accessToken) && (StringUtils.hasText(externalUserId) || StringUtils.hasText(pageId));
        return new NormalizedRequest(enabled, defaultApiUrl(platform), accessToken, refreshToken, externalUserId, pageId,
                username, pageUrl, businessPageLink, displayName);
    }

    private Optional<ExternalPlatform> findOrgPlatformRecord(Long orgId, Platform platform) {
        for (String platformName : platformNames(platform)) {
            Optional<ExternalPlatform> record = externalPlatformRepository.findByOrganizationIdAndPlatformNameIgnoreCase(orgId, platformName);
            if (record.isPresent()) {
                return record;
            }
        }
        return Optional.empty();
    }

    private boolean shouldReplaceWithLegacy(ExternalPlatformPayload payload) {
        if (!payload.connected()) {
            return true;
        }
        if (!StringUtils.hasText(payload.accessToken)) {
            return true;
        }
        return looksLikePlaceholderToken(payload.accessToken);
    }

    private boolean looksLikePlaceholderToken(String token) {
        if (!StringUtils.hasText(token)) {
            return true;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("abc123")
                || normalized.equals("xyz321")
                || normalized.equals("ghi789")
                || normalized.equals("def456")
                || normalized.equals("token");
    }

    private ObjectNode getLegacyPlatformNode(ObjectNode rootNode, Platform platform) {
        if (rootNode == null) {
            return null;
        }
        String platformKey = LEGACY_PLATFORM_KEYS.get(platform);
        JsonNode node = rootNode.get(platformKey);
        if (node == null && platform == Platform.X) {
            node = rootNode.get("x");
            if (node == null) {
                node = rootNode.get("twitter");
            }
        }
        if (node instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return null;
    }

    private String legacyValue(ObjectNode node, String... fields) {
        if (node == null || fields == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode valueNode = node.get(field);
            if (valueNode != null && !valueNode.isNull()) {
                String value = valueNode.asText();
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private String defaultPageUrl(Platform platform, String username, String pageId, String externalUserId) {
        return switch (platform) {
            case FACEBOOK -> StringUtils.hasText(pageId) ? "https://www.facebook.com/" + pageId : null;
            case INSTAGRAM -> StringUtils.hasText(username) ? "https://www.instagram.com/" + username + "/" : null;
            case LINKEDIN -> StringUtils.hasText(externalUserId) ? "https://www.linkedin.com/company/" + externalUserId : null;
            case X -> StringUtils.hasText(username) ? "https://x.com/" + username : null;
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isTransactionExpired(java.time.LocalDateTime transactionCreatedAt) {
        return transactionCreatedAt != null
                && transactionCreatedAt.plusMinutes(TRANSACTION_TTL_MINUTES).isBefore(java.time.LocalDateTime.now());
    }

    private String generateTransactionId(Long organizationId, Platform platform) {
        return organizationId + ":" + platform.name() + ":" + UUID.randomUUID();
    }

    private Long resolveOrganizationId(UserAuthToken token) {
        if (token == null || !StringUtils.hasText(token.getTransactionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization context missing for business page flow");
        }
        String[] segments = token.getTransactionId().split(":");
        if (segments.length < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid business page transaction");
        }
        try {
            return Long.parseLong(segments[0]);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid organization context for business page flow");
        }
    }

    private String orgCallbackUrl(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> publicUrlProperties.getOrganizationFacebookCallbackUrl();
            case INSTAGRAM -> publicUrlProperties.getOrganizationInstagramCallbackUrl();
            case LINKEDIN -> publicUrlProperties.getOrganizationLinkedInCallbackUrl();
            case X -> publicUrlProperties.getOrganizationXCallbackUrl();
        };
    }

    private Collection<String> platformNames(Platform platform) {
        if (platform == Platform.X) {
            return List.of(platform.getDisplayName().toUpperCase(), "TWITTER", "X");
        }
        return List.of(platform.getDisplayName().toUpperCase(), platform.name());
    }

    private String defaultApiUrl(Platform platform) {
        return switch (platform) {
            case FACEBOOK, INSTAGRAM -> "https://graph.facebook.com";
            case LINKEDIN -> "https://api.linkedin.com";
            case X -> "https://api.twitter.com";
        };
    }

    private String startLinkedInBusinessConnect(UserDetailsImpl userDetails) {
        ensureOrgAdmin(userDetails);
        OrgBusinessConnectTransaction transaction = createTransaction(userDetails, Platform.LINKEDIN);
        orgBusinessConnectTransactionRepository.save(transaction);
        String callbackUrl = publicUrlProperties.getOrganizationLinkedInCallbackUrl();
        if (!StringUtils.hasText(callbackUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn organization callback URL is not configured");
        }
        log.info("Starting LinkedIn organization business connect | orgId={} | transactionId={} | callbackUrl={}",
                userDetails.getOrganization().getId(), transaction.getTransactionId(), callbackUrl);
        return linkedInOrganizationOAuthService.buildOrgAuthorizationUrl(callbackUrl, transaction.getTransactionId());
    }

    private void completeLinkedInBusinessConnect(String code, String transactionId) {
        if (!StringUtils.hasText(transactionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LinkedIn business page callback missing state/transactionId");
        }
        OrgBusinessConnectTransaction transaction = getActiveTransaction(transactionId, Platform.LINKEDIN);
        String callbackUrl = publicUrlProperties.getOrganizationLinkedInCallbackUrl();
        LinkedInOrganizationOAuthService.LinkedInDiscoveryResult result =
                linkedInOrganizationOAuthService.exchangeAndDiscoverWithoutSignIn(code, callbackUrl);

        if (result.pages() == null || result.pages().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No LinkedIn organization pages discovered");
        }

        // Store discovered pages + token in transaction for selection step
        transaction.setTemporaryAccessToken(result.accessToken());
        transaction.setTemporaryRefreshToken(result.refreshToken());
        transaction.setDiscoveredPagesJson(writeAvailablePages(result.pages()));
        transaction.setStatus(TX_STATUS_READY);
        orgBusinessConnectTransactionRepository.save(transaction);
        log.info("LinkedIn business pages discovered | orgId={} | count={}", transaction.getOrganizationId(), result.pages().size());
    }

    private String startXBusinessConnect(UserDetailsImpl userDetails, HttpSession session) {
        ensureOrgAdmin(userDetails);
        OrgBusinessConnectTransaction transaction = createTransaction(userDetails, Platform.X);
        orgBusinessConnectTransactionRepository.save(transaction);
        String callbackBaseUrl = publicUrlProperties.getOrganizationXCallbackUrl();
        if (!StringUtils.hasText(callbackBaseUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "X organization callback URL is not configured");
        }
        String callbackUrl = callbackBaseUrl;
        log.info("Starting X organization business connect | orgId={} | transactionId={} | callbackUrl={}",
                userDetails.getOrganization().getId(), transaction.getTransactionId(), callbackUrl);
        try {
            XConnectionService.OrgAuthorizationStart authStart = xConnectionService.beginOrganizationAuthorization(callbackUrl, session);
            transaction.setTemporaryAccessToken(authStart.requestToken());
            transaction.setTemporaryAccessSecret(authStart.requestTokenSecret());
            orgBusinessConnectTransactionRepository.save(transaction);
            return authStart.authorizationUrl();
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to start X business connection: " + ex.getMessage(), ex);
        }
    }

    private void putIfPresent(ObjectNode root, String field, String value) {
        if (StringUtils.hasText(value)) {
            root.put(field, value.trim());
        }
    }

    private ResponseStatusException linkedInManualOnlyException() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "LinkedIn business-page OAuth is not supported. Use manual company setup."
        );
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record NormalizedRequest(
            boolean enabled,
            String apiUrl,
            String accessToken,
            String refreshToken,
            String externalUserId,
            String pageId,
            String username,
            String pageUrl,
            String businessPageLink,
            String displayName
    ) {
    }

    private boolean containsLegacyBusinessPageNodes(ObjectNode root) {
        if (root == null) {
            return false;
        }
        return root.has("facebook") || root.has("instagram") || root.has("linkedin") || root.has("x") || root.has("twitter");
    }

    private void removeBusinessPageNodes(ObjectNode root) {
        if (root == null) {
            return;
        }
        root.remove("facebook");
        root.remove("instagram");
        root.remove("linkedin");
        root.remove("x");
        root.remove("twitter");
    }

    @Getter
    @Builder
    public static class ResolvedOrgPlatformConfig {
        private final Long organizationId;
        private final Platform platform;
        private final String apiUrl;
        private final String accessToken;
        private final String refreshToken;
        private final String externalUserId;
        private final String pageId;
        private final String username;
        private final String pageUrl;
        private final String businessPageLink;
        private final String displayName;

        public boolean isUsable() {
            return StringUtils.hasText(accessToken) && (StringUtils.hasText(externalUserId) || StringUtils.hasText(pageId));
        }

        public String resolvedExternalId() {
            if (StringUtils.hasText(externalUserId)) {
                return externalUserId;
            }
            return pageId;
        }
    }

    @Getter
    @Builder
    public static class ResolvedLeaderPlatformConfig {
        private final Long userId;
        private final Long organizationId;
        private final Platform platform;
        private final String apiUrl;
        private final String accessToken;
        private final String accessSecret;
        private final String refreshToken;
        private final String externalUserId;
        private final String clientId;

        public boolean isUsable() {
            return StringUtils.hasText(accessToken) && StringUtils.hasText(externalUserId);
        }
    }

    private static final class ExternalPlatformPayload {
        private final Long organizationId;
        private final Platform platform;
        private final boolean enabled;
        private final String apiUrl;
        private final String accessToken;
        private final String refreshToken;
        private final String externalUserId;
        private final String pageId;
        private final String username;
        private final String pageUrl;
        private final String businessPageLink;
        private final String displayName;
        private final LocalDateTime lastSyncAttemptAt;
        private final LocalDateTime lastSyncSuccessAt;
        private final String lastSyncStatus;
        private final String lastSyncError;
        private final LocalDateTime lastSyncErrorAt;
        private final Integer lastImportedCount;

        private ExternalPlatformPayload(Long organizationId, Platform platform, boolean enabled, String apiUrl, String accessToken,
                                        String refreshToken, String externalUserId, String pageId, String username,
                                        String pageUrl, String businessPageLink, String displayName,
                                        LocalDateTime lastSyncAttemptAt, LocalDateTime lastSyncSuccessAt,
                                        String lastSyncStatus, String lastSyncError, LocalDateTime lastSyncErrorAt,
                                        Integer lastImportedCount) {
            this.organizationId = organizationId;
            this.platform = platform;
            this.enabled = enabled;
            this.apiUrl = apiUrl;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.externalUserId = externalUserId;
            this.pageId = pageId;
            this.username = username;
            this.pageUrl = pageUrl;
            this.businessPageLink = businessPageLink;
            this.displayName = displayName;
            this.lastSyncAttemptAt = lastSyncAttemptAt;
            this.lastSyncSuccessAt = lastSyncSuccessAt;
            this.lastSyncStatus = StringUtils.hasText(lastSyncStatus) ? lastSyncStatus : SYNC_STATUS_NEVER_SYNCED;
            this.lastSyncError = lastSyncError;
            this.lastSyncErrorAt = lastSyncErrorAt;
            this.lastImportedCount = lastImportedCount;
        }

        static ExternalPlatformPayload from(ExternalPlatform record, Platform platform) {
            JsonNode node;
            try {
                node = StringUtils.hasText(record.getCredentials()) ? OBJECT_MAPPER.readTree(record.getCredentials()) : OBJECT_MAPPER.createObjectNode();
            } catch (Exception ignored) {
                node = OBJECT_MAPPER.createObjectNode();
            }
            return new ExternalPlatformPayload(
                    record.getOrganizationId(),
                    platform,
                    Boolean.TRUE.equals(record.getEnabled()),
                    record.getApiUrl(),
                    firstNonBlank(text(node, "accessToken"), text(node, "token")),
                    text(node, "refreshToken"),
                    firstNonBlank(text(node, "externalUserId"), text(node, "userId")),
                    text(node, "pageId"),
                    text(node, "username"),
                    firstNonBlank(text(node, "pageUrl"), text(node, "url"), text(node, "pageLink")),
                    firstNonBlank(text(node, "businessPageLink"), text(node, "business_page_link"),
                            text(node, "pageUrl"), text(node, "url"), text(node, "pageLink")),
                    firstNonBlank(text(node, "displayName"), text(node, "pageName"), text(node, "name")),
                    record.getLastSyncAttemptAt(),
                    record.getLastSyncSuccessAt(),
                    record.getLastSyncStatus(),
                    record.getLastSyncError(),
                    record.getLastSyncErrorAt(),
                    record.getLastImportedCount()
            );
        }

        static ExternalPlatformPayload empty(Platform platform) {
            return new ExternalPlatformPayload(null, platform, false, null, null, null, null, null, null, null, null, null,
                    null, null, SYNC_STATUS_NEVER_SYNCED, null, null, 0);
        }

        boolean connected() {
            return StringUtils.hasText(accessToken) && (StringUtils.hasText(externalUserId) || StringUtils.hasText(pageId));
        }

        // NOTE: keep this logic in sync with OrgBusinessPageContentService.resolveSuffix
        // and PlatformBusinessPageComposer.resolveSuffix. Tracked tech debt: consolidate into one.
        String contentSuffix() {
            if (platform == null) {
                return "";
            }
            return switch (platform) {
                case X, INSTAGRAM -> StringUtils.hasText(username)
                        ? "@" + username
                        : firstNonBlank(businessPageLink, pageUrl);
                case FACEBOOK -> StringUtils.hasText(username)
                        ? "@" + username
                        : (StringUtils.hasText(firstNonBlank(businessPageLink, pageUrl))
                                ? "Follow Us: " + firstNonBlank(businessPageLink, pageUrl) : "");
                case LINKEDIN -> {
                    String url = firstNonBlank(businessPageLink, pageUrl);
                    yield StringUtils.hasText(url) ? "Follow Us: " + url : "";
                }
            };
        }

        private static String text(JsonNode node, String field) {
            if (node == null || node.isNull()) {
                return null;
            }
            JsonNode fieldNode = node.get(field);
            if (fieldNode == null || fieldNode.isNull()) {
                return null;
            }
            String value = fieldNode.asText();
            return StringUtils.hasText(value) ? value.trim() : null;
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
            return null;
        }
    }
}
