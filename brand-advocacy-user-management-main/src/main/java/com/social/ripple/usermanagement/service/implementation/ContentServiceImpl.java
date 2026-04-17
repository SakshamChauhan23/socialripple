/**
 * Filename: ContentServiceImpl.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.usermanagement.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.usermanagement.dao.model.ConfigParameter;
import com.social.ripple.usermanagement.dao.model.Organization;
import com.social.ripple.usermanagement.dao.model.TrendingHashtags;
import com.social.ripple.usermanagement.dao.repository.OrganizationRepository;
import com.social.ripple.usermanagement.dao.repository.TrendingHashtagsRepository;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dto.request.ContentGenerateRequestDTO;
import com.social.ripple.usermanagement.dto.request.TrendingTopicsRequestDTO;
import com.social.ripple.usermanagement.dto.response.ContentGenerateResponse;
import com.social.ripple.usermanagement.dto.response.ContentGenerateResponseDTO;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ContentLimitCalculator;
import com.social.ripple.usermanagement.service.GeminiClient;
import com.social.ripple.usermanagement.service.IContentService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class ContentServiceImpl implements IContentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CUSTOM_TRENDING_TOPICS_KEY = "customTrendingTopics";
    private static final String GENERATED_TRENDING_TOPICS_KEY = "generatedTrendingTopics";

    private static final String DEFAULT_NEWSAPI_BASE_URL = "https://newsapi.org/v2/everything";

    private final GeminiClient geminiClient;
    private final ContentLimitCalculator contentLimitCalculator;

    @Autowired
    private TrendingHashtagsRepository trendingHashtagsRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public ContentServiceImpl(GeminiClient geminiClient, ContentLimitCalculator contentLimitCalculator) {
        this.geminiClient = geminiClient;
        this.contentLimitCalculator = contentLimitCalculator;
    }

    @Override
    public ContentGenerateResponse generateContent(String traceId, String correlationId, String tenantId,
                                                    UserDetailsImpl userDetails, ContentGenerateRequestDTO requestDTO) {
        log.info("[{}]|CONTENT_SERVICE|Start generateContent for topic: {}", traceId, requestDTO.getTopic());

        ContentGenerateResponse response = new ContentGenerateResponse();
        try {
            // Compute per-platform character budgets from the org's actual connected pages
            Long orgId = (userDetails != null && userDetails.getOrganization() != null)
                    ? userDetails.getOrganization().getId() : null;
            ContentLimitCalculator.PlatformBudgets budgets = contentLimitCalculator.calculateFor(orgId);
            int commonMax = budgets.commonMax();
            int xMax = resolveXMaxCharacters(requestDTO.getXMaxCharacters(), budgets.xMax());
            log.info("[{}]|CONTENT_SERVICE|Budgets for orgId={}: commonMax={}, xMax={}", traceId, orgId, commonMax, xMax);

            String prompt = String.format("Role: You are an expert LinkedIn Copywriter and Social Media Strategist. You specialize in creating viral, professional, and engaging content that drives connections and comments.\n" +
                    "\n" +
                    "Task: Write a LinkedIn post based on the following topic.\n" +
                    "\n" +
                    "Input Topic: %s \n" +
                    "\n" +
                    "Instructions:\n" +
                    "\n" +
                    "Hook: Start with a catchy, punchy headline or opening line (a \"hook\") to grab attention immediately.\n" +
                    "\n" +
                    "Body: Expand on the topic with value-driven insights. Use short paragraphs (1-2 sentences) to make it readable on mobile.\n" +
                    "\n" +
                    "Tone: Keep the tone professional yet authentic and conversational (Human-like). Avoid overly robotic or \"salesy\" language.\n" +
                    "\n" +
                    "Formatting: Use bullet points to break up the text and make it visually appealing.\n" +
                    "\n" +
                    "Engagement: End with a clear Call to Action (CTA) or a question to encourage comments.\n" +
                    "\n" +
                    "Hashtags: Include 3-5 relevant, high-traffic hashtags at the very bottom.\n" +
                    "\n" +
                    "Constraints:\n" +
                    "\n" +
                    "The 'common' content is published to LinkedIn, Facebook, AND Instagram. " +
                    "Keep 'common' strictly under " + commonMax + " characters (including hashtags). " +
                    "The remaining characters up to each platform's limit are reserved for a business " +
                    "page link and handle that will be appended automatically before publishing.\n" +
                    "\n" +
                    "Do not use jargon unless necessary.\n" +
                    "\n" +
                    "Make the first 2 lines count (before the \"See more\" fold). Do not use emojis. Use only UTF-8 characters. Do not include the introduction. Do not use bold and italic. " +
                    "Prepare separate content for common and X.\n\n" +
                    "The final published post structure for ALL platforms is:\n" +
                    "  content\\n@business_page_handle\\nhashtags\n" +
                    "The business page handle is appended automatically — do NOT include any business page handle, @mention, or URL link in your output.\n\n" +
                    "For common: generate content text with 3-5 hashtags at the bottom. Keep strictly under " + commonMax + " characters total.\n\n" +
                    "For X: generate content text with 2-3 relevant hashtags at the bottom, all within " + xMax + " characters total. " +
                    "The remaining " + (280 - xMax) + " characters are reserved for the business page handle that is appended automatically.\n\n" +
                    "Response needed in two fields: \"common\" and \"xOnly\"", requestDTO.getTopic());


            ContentGenerateResponseDTO dto = geminiClient.generateText(traceId, prompt);

            if (dto.getGeneratedText() != null && charCount(dto.getGeneratedText()) > commonMax) {
                log.warn("[{}]|CONTENT_SERVICE|common content exceeded budget ({} > {})", traceId, charCount(dto.getGeneratedText()), commonMax);
            }
            if (dto.getXGeneratedContent() != null && charCount(dto.getXGeneratedContent()) > xMax) {
                log.warn("[{}]|CONTENT_SERVICE|X content exceeded budget ({} > {})", traceId, charCount(dto.getXGeneratedContent()), xMax);
            }

            response.setContentGenerateResponseDTO(dto);
            response.setStatus(true);
            response.setCode(ResponseCode.USMG_200);
            response.setMessage("Content generated successfully");

        } catch (Exception e) {
            log.error("[{}]|CONTENT_SERVICE|Error generating content: {}", traceId, e.getMessage(), e);
            response.setCode(ResponseCode.USMG_500);
            response.setMessage("Failed to generate content");
            response.setDevMessage(e.getMessage());

        }
        return response;
    }

    private int resolveXMaxCharacters(Integer xMaxCharacters, int calculatedMax) {
        if (xMaxCharacters != null && xMaxCharacters > 0 && xMaxCharacters <= 280) {
            return Math.min(xMaxCharacters, calculatedMax);
        }
        return calculatedMax;
    }

    private static int charCount(String text) {
        return text == null ? 0 : text.codePointCount(0, text.length());
    }

    public void fetchTrendingHashtags(String traceId) {
        log.info("[{}]|CONTENT_SERVICE|Start fetchTrendingHashtags", traceId);

        ContentGenerateResponse response = new ContentGenerateResponse();
        try {

            String prompt = "get the top 15 trending hash tags in social media. Exactly as comma separated without introduction and footer.";

            String generatedText = geminiClient.generateTextFlash(traceId, prompt);

            log.info("Generated text:{}", generatedText);

            TrendingHashtags trendingHashtags = new TrendingHashtags();

            trendingHashtags.setHashtags(generatedText);

            trendingHashtagsRepository.save(trendingHashtags);

        } catch (Exception e) {
            log.error("[{}]|CONTENT_SERVICE|Error generating content: {}", traceId, e.getMessage(), e);

        }
    }

    public void fetchTrendingTopics(String traceId) {
        log.info("[{}]|CONTENT_SERVICE|Start fetchTrendingHashtags", traceId);
        try {
            refreshDefaultTrendingTopics(traceId);

        } catch (Exception e) {
            log.error("[{}]|CONTENT_SERVICE|Error generating content: {}", traceId, e.getMessage(), e);

        }
    }

    public String getBusinessTopicsAsString() {
        try {
            String apiKey = readConfigValue(ConfigKeys.NEWSAPI_API_KEY);
            if (!StringUtils.hasText(apiKey)) {
                log.warn("NEWSAPI_API_KEY is not configured; trending topics from NewsAPI are unavailable.");
                return "Error: NewsAPI key not configured";
            }
            String baseUrl = readConfigValue(ConfigKeys.NEWSAPI_BASE_URL);
            if (!StringUtils.hasText(baseUrl)) {
                baseUrl = DEFAULT_NEWSAPI_BASE_URL;
            }

            // Build the URL with parameters using the non-deprecated method
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("q", "technology OR business")
                    .queryParam("sortBy", "publishedAt")
                    .queryParam("language", "en")
                    .queryParam("pageSize", 10)
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();

            // Make the API call
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null) {
                return "Error: No response from API";
            }

            // Check if the request was successful
            if (!response.has("status") || !"ok".equals(response.get("status").asText())) {
                String errorMessage = response.has("message") ?
                        response.get("message").asText() : "Unknown error";
                return "Error: " + errorMessage;
            }

            // Extract articles
            JsonNode articles = response.get("articles");
            if (articles == null || !articles.isArray()) {
                return "No articles found";
            }

            // Collect titles/topics
            List<String> topics = new ArrayList<>();
            for (JsonNode article : articles) {
                if (article.has("title") && !article.get("title").isNull()) {
                    String title = article.get("title").asText().trim();
                    if (!title.isEmpty()) {
                        topics.add(title);
                    }
                }
            }

            // Join with ### separator
            return String.join("##", topics);

        } catch (Exception e) {
            return "Error calling NewsAPI: " + e.getMessage();
        }
    }

    private String readConfigValue(String key) {
        ConfigParameter param = AppCache.configParameters.get(key);
        return param != null ? param.getConfigValue() : null;
    }

    private List<String> parseTrendingTopics(String topicsValue) {
        if (!StringUtils.hasText(topicsValue)) {
            return List.of();
        }

        // Strip outer brackets if Gemini wraps entire response in [...]
        String cleaned = topicsValue.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return Arrays.stream(cleaned.split("##|\\r?\\n"))
                .map(topic -> topic.replaceFirst("^[-*\\d.\\s]+", "")
                        .replaceAll("^\\[|\\]$", "")      // strip leading [ and trailing ]
                        .replaceAll("^\"|\"$", "")          // strip leading/trailing quotes
                        .replaceAll("^'|'$", "")            // strip leading/trailing single quotes
                        .trim())
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public ContentGenerateResponse getTrendingHashtags(String traceId, String correlationId, String tenantId) {
        ContentGenerateResponse contentGenerateResponse = new ContentGenerateResponse();

        Optional<TrendingHashtags> trendingHashtagsFetch =  trendingHashtagsRepository.findFirstByHashtagsIsNotNullOrderByUpdatedAtDesc();

        if(trendingHashtagsFetch.isPresent()){
            TrendingHashtags trendingHashtagsEntity = trendingHashtagsFetch.get();
            if(StringUtils.hasText(trendingHashtagsEntity.getHashtags())) {
                List<String> trendingHashtags = Arrays.asList(trendingHashtagsEntity.getHashtags().split(","));
                contentGenerateResponse.setTrendingHashtags(trendingHashtags);
            }
        }

        return contentGenerateResponse;
    }

    @Override
    public ContentGenerateResponse getTrendingTopics(String traceId, String correlationId, String tenantId) {
        ContentGenerateResponse contentGenerateResponse = new ContentGenerateResponse();

        List<String> generatedTopics = resolveTrendingTopics(traceId, tenantId, false);
        contentGenerateResponse.setTrendingTopics(generatedTopics);

        contentGenerateResponse.setStatus(true);
        contentGenerateResponse.setCode(ResponseCode.USMG_200);
        contentGenerateResponse.setMessage("Trending topics fetched successfully");
        return contentGenerateResponse;
    }

    @Override
    public ContentGenerateResponse getTrendingTopicPreferences(String traceId, String correlationId, String tenantId) {
        ContentGenerateResponse response = new ContentGenerateResponse();
        response.setTrendingTopics(getCustomTopicsFromOrganization(tenantId));
        response.setStatus(true);
        response.setCode(ResponseCode.USMG_200);
        response.setMessage("Trending topic preferences fetched successfully");
        return response;
    }

    @Override
    public ContentGenerateResponse createCustomTrendingTopics(String traceId, String correlationId, String tenantId,
                                                              UserDetailsImpl userDetails, TrendingTopicsRequestDTO requestDTO) {
        return saveCustomTrendingTopics(traceId, tenantId, userDetails, requestDTO, false);
    }

    @Override
    public ContentGenerateResponse updateCustomTrendingTopics(String traceId, String correlationId, String tenantId,
                                                              UserDetailsImpl userDetails, TrendingTopicsRequestDTO requestDTO) {
        return saveCustomTrendingTopics(traceId, tenantId, userDetails, requestDTO, true);
    }

    private ContentGenerateResponse saveCustomTrendingTopics(String traceId, String tenantId, UserDetailsImpl userDetails,
                                                             TrendingTopicsRequestDTO requestDTO, boolean allowClear) {
        ContentGenerateResponse response = new ContentGenerateResponse();

        if (userDetails == null || userDetails.getOrganization() == null || userDetails.getOrganization().getId() == null) {
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_401);
            response.setMessage("Unauthorized request");
            response.setDevMessage("User details missing");
            return response;
        }

        Long userOrgId = userDetails.getOrganization().getId();
        if (!StringUtils.hasText(tenantId) || !tenantId.equals(String.valueOf(userOrgId))) {
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_403);
            response.setMessage("Access denied for this tenant");
            response.setDevMessage("Tenant mismatch");
            return response;
        }

        if (requestDTO == null) {
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_400);
            response.setMessage("Invalid request body");
            return response;
        }

        List<String> cleanedTopics = sanitizeTopics(requestDTO.getTopics());
        if (cleanedTopics.isEmpty() && !allowClear) {
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_400);
            response.setMessage("Topics list cannot be empty");
            return response;
        }

        Optional<Organization> organizationOptional = organizationRepository.findById(userOrgId);
        if (organizationOptional.isEmpty()) {
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_404);
            response.setMessage("Organization not found");
            return response;
        }

        Organization organization = organizationOptional.get();
        try {
            ObjectNode rootNode = parseOrganizationConfig(organization.getSocialHandleConfig());

            if (cleanedTopics.isEmpty()) {
                rootNode.remove(CUSTOM_TRENDING_TOPICS_KEY);
                rootNode.remove(GENERATED_TRENDING_TOPICS_KEY);
            } else {
                ArrayNode topicsNode = rootNode.putArray(CUSTOM_TRENDING_TOPICS_KEY);
                for (String topic : cleanedTopics) {
                    topicsNode.add(topic);
                }
            }

            organization.setSocialHandleConfig(rootNode.toString());
            organizationRepository.save(organization);
        } catch (Exception ex) {
            log.error("[{}]|CONTENT_SERVICE|Failed to save custom trending topics: {}", traceId, ex.getMessage(), ex);
            response.setStatus(false);
            response.setCode(ResponseCode.USMG_500);
            response.setMessage("Failed to save custom trending topics");
            response.setDevMessage(ex.getMessage());
            return response;
        }

        List<String> generatedTopics = resolveTrendingTopics(traceId, tenantId, true);
        response.setStatus(true);
        response.setCode(ResponseCode.USMG_200);
        response.setTrendingTopics(generatedTopics);
        response.setMessage(cleanedTopics.isEmpty()
                ? "Custom trending topics cleared and regenerated successfully"
                : "Custom trending topics saved and regenerated successfully");
        return response;
    }

    private List<String> resolveTrendingTopics(String traceId, String tenantId, boolean forceRefresh) {
        List<String> customTopics = getCustomTopicsFromOrganization(tenantId);
        if (!customTopics.isEmpty()) {
            if (!forceRefresh) {
                List<String> cachedGeneratedTopics = getGeneratedTopicsFromOrganization(tenantId);
                if (!cachedGeneratedTopics.isEmpty()) {
                    return cachedGeneratedTopics;
                }
            }

            List<String> generatedTopics = generateTopicsFromPreferences(traceId, customTopics);
            saveGeneratedTopicsToOrganization(tenantId, generatedTopics);
            return generatedTopics;
        }

        return forceRefresh ? refreshDefaultTrendingTopics(traceId) : getOrRefreshDefaultTrendingTopics(traceId);
    }

    private List<String> getCustomTopicsFromOrganization(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return List.of();
        }

        Long organizationId;
        try {
            organizationId = Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            return List.of();
        }

        Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
        if (organizationOptional.isEmpty()) {
            return List.of();
        }

        String socialHandleConfig = organizationOptional.get().getSocialHandleConfig();
        if (!StringUtils.hasText(socialHandleConfig)) {
            return List.of();
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(socialHandleConfig);
            JsonNode topicsNode = rootNode.get(CUSTOM_TRENDING_TOPICS_KEY);
            if (topicsNode == null || !topicsNode.isArray()) {
                return List.of();
            }

            List<String> topics = new ArrayList<>();
            for (JsonNode topicNode : topicsNode) {
                if (topicNode != null && topicNode.isTextual()) {
                    String topic = topicNode.asText().trim();
                    if (!topic.isEmpty()) {
                        topics.add(topic);
                    }
                }
            }
            return topics;
        } catch (Exception ex) {
            log.warn("Failed to parse custom trending topics for tenant {}: {}", tenantId, ex.getMessage());
            return List.of();
        }
    }

    private List<String> getGeneratedTopicsFromOrganization(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return List.of();
        }

        Long organizationId;
        try {
            organizationId = Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            return List.of();
        }

        Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
        if (organizationOptional.isEmpty()) {
            return List.of();
        }

        String socialHandleConfig = organizationOptional.get().getSocialHandleConfig();
        if (!StringUtils.hasText(socialHandleConfig)) {
            return List.of();
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(socialHandleConfig);
            JsonNode topicsNode = rootNode.get(GENERATED_TRENDING_TOPICS_KEY);
            if (topicsNode == null || !topicsNode.isArray()) {
                return List.of();
            }

            List<String> topics = new ArrayList<>();
            for (JsonNode topicNode : topicsNode) {
                if (topicNode != null && topicNode.isTextual()) {
                    String topic = topicNode.asText().trim();
                    if (!topic.isEmpty()) {
                        topics.add(topic);
                    }
                }
            }
            return topics;
        } catch (Exception ex) {
            log.warn("Failed to parse generated trending topics for tenant {}: {}", tenantId, ex.getMessage());
            return List.of();
        }
    }

    private void saveGeneratedTopicsToOrganization(String tenantId, List<String> generatedTopics) {
        if (!StringUtils.hasText(tenantId)) {
            return;
        }

        Long organizationId;
        try {
            organizationId = Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            return;
        }

        Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
        if (organizationOptional.isEmpty()) {
            return;
        }

        Organization organization = organizationOptional.get();
        try {
            ObjectNode rootNode = parseOrganizationConfig(organization.getSocialHandleConfig());
            rootNode.remove(GENERATED_TRENDING_TOPICS_KEY);

            if (!generatedTopics.isEmpty()) {
                ArrayNode topicsNode = rootNode.putArray(GENERATED_TRENDING_TOPICS_KEY);
                for (String topic : generatedTopics) {
                    topicsNode.add(topic);
                }
            }

            organization.setSocialHandleConfig(rootNode.toString());
            organizationRepository.save(organization);
        } catch (Exception ex) {
            log.warn("Failed to save generated trending topics for tenant {}: {}", tenantId, ex.getMessage());
        }
    }

    private List<String> getOrRefreshDefaultTrendingTopics(String traceId) {
        try {
            Optional<TrendingHashtags> trendingHashtagsFetch = trendingHashtagsRepository.findFirstByTopicsIsNotNullOrderByUpdatedAtDesc();

            if (trendingHashtagsFetch.isPresent()) {
                TrendingHashtags trendingHashtagsEntity = trendingHashtagsFetch.get();
                if (StringUtils.hasText(trendingHashtagsEntity.getTopics())) {
                    List<String> cachedTopics = parseTrendingTopics(trendingHashtagsEntity.getTopics());
                    if (!cachedTopics.isEmpty()) {
                        return cachedTopics;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("[{}]|CONTENT_SERVICE|Failed to fetch trending topics from trending_hashtags: {}", traceId, ex.getMessage());
        }

        return refreshDefaultTrendingTopics(traceId);
    }

    private List<String> refreshDefaultTrendingTopics(String traceId) {
        List<String> generatedTopicList = parseTrendingTopics(getBusinessTopicsAsString());
        if (generatedTopicList.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            TrendingHashtags trendingHashtags = new TrendingHashtags();
            trendingHashtags.setTopics(String.join("##", generatedTopicList));
            trendingHashtags.setUpdatedAt(LocalDateTime.now());
            trendingHashtagsRepository.save(trendingHashtags);
        } catch (Exception ex) {
            log.warn("[{}]|CONTENT_SERVICE|Failed to cache generated trending topics: {}", traceId, ex.getMessage());
        }

        return generatedTopicList;
    }

    private List<String> generateTopicsFromPreferences(String traceId, List<String> preferenceTopics) {
        if (preferenceTopics == null || preferenceTopics.isEmpty()) {
            return refreshDefaultTrendingTopics(traceId);
        }

        try {
            String prompt = String.format(
                    "Use Google Search to find what is actually trending in the news and on social media RIGHT NOW (today's date) " +
                            "about these focus areas: %s. " +
                            "Return exactly 10 current trending discussion topics. Each topic must be a short specific headline " +
                            "tied to a real, recent event or development (not a generic theme), suitable for employee advocacy content " +
                            "planning. Return only the topics separated by ## with no numbering, bullets, intro, footer, or citation markers.",
                    String.join(", ", preferenceTopics)
            );
            List<String> generatedTopics = parseTrendingTopics(geminiClient.generateTextFlashGrounded(traceId, prompt));
            if (!generatedTopics.isEmpty()) {
                return generatedTopics;
            }
        } catch (Exception ex) {
            log.warn("[{}]|CONTENT_SERVICE|Failed to generate preference-based trending topics: {}", traceId, ex.getMessage());
        }

        return refreshDefaultTrendingTopics(traceId);
    }

    private ObjectNode parseOrganizationConfig(String socialHandleConfig) throws Exception {
        if (!StringUtils.hasText(socialHandleConfig)) {
            return OBJECT_MAPPER.createObjectNode();
        }

        JsonNode rootNode = OBJECT_MAPPER.readTree(socialHandleConfig);
        if (rootNode == null || rootNode.isNull()) {
            return OBJECT_MAPPER.createObjectNode();
        }
        if (rootNode.isObject()) {
            return (ObjectNode) rootNode;
        }
        throw new IllegalArgumentException("Invalid social_handle_config format");
    }

    private List<String> sanitizeTopics(List<String> inputTopics) {
        if (inputTopics == null || inputTopics.isEmpty()) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String topic : inputTopics) {
            if (topic == null) {
                continue;
            }
            String trimmed = topic.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return new ArrayList<>(normalized);
    }
}
