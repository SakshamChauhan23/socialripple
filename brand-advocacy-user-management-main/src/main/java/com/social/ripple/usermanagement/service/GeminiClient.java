/**
 * Filename: GeminiClient.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.usermanagement.dto.response.ContentGenerateResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeminiClient {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private final RestTemplate restTemplate;
    private final AppCache appCache;
    private final String geminiApiKeyOverride;
    private final String geminiApiUrlOverride;
    private final String geminiFlashApiUrlOverride;

    @Autowired
    public GeminiClient(
            RestTemplate restTemplate,
            AppCache appCache,
            @Value("${app.ai.gemini.api-key:}") String geminiApiKeyOverride,
            @Value("${app.ai.gemini.api-url:}") String geminiApiUrlOverride,
            @Value("${app.ai.gemini.flash-api-url:}") String geminiFlashApiUrlOverride) {
        this.restTemplate = restTemplate;
        this.appCache = appCache;
        this.geminiApiKeyOverride = geminiApiKeyOverride;
        this.geminiApiUrlOverride = geminiApiUrlOverride;
        this.geminiFlashApiUrlOverride = geminiFlashApiUrlOverride;
    }

    public ContentGenerateResponseDTO generateText(String traceId, String prompt) {
        log.info("[{}]|GEMINI_CLIENT|Start external AI call with flash-first strategy", traceId);

        try {
            GeminiConfig config = getGeminiConfig(traceId);
            String flashUrl = Optional.ofNullable(geminiFlashApiUrlOverride)
                    .filter(value -> !value.isBlank())
                    .orElse("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
            return invokeGemini(traceId, prompt, flashUrl, config.apiKey(), "flash");
        }
        catch (HttpClientErrorException e) {
            log.error("[{}]|GEMINI_CLIENT|Flash call failed with status code {}: {}",
                    traceId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (shouldFallbackToPro(e.getStatusCode())) {
                log.warn("[{}]|GEMINI_CLIENT|Falling back to pro model after flash model failure", traceId);
                return generateTextWithProFallback(traceId, prompt);
            }
            throw new RuntimeException(buildUserFriendlyError(e.getStatusCode().value(), e.getResponseBodyAsString()), e);
        }
        catch (HttpServerErrorException e) {
            log.error("[{}]|GEMINI_CLIENT|Flash call failed with server error {}: {}",
                    traceId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (shouldFallbackToPro(e.getStatusCode())) {
                log.warn("[{}]|GEMINI_CLIENT|Falling back to pro model after flash model server error", traceId);
                return generateTextWithProFallback(traceId, prompt);
            }
            throw new RuntimeException(buildUserFriendlyError(e.getStatusCode().value(), e.getResponseBodyAsString()), e);
        }
        catch (Exception e) {
            log.error("[{}]|GEMINI_CLIENT|AI call failed: {}", traceId, e.getMessage(), e);
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Gemini AI call failed", e);
        }
    }

    private ContentGenerateResponseDTO generateTextWithProFallback(String traceId, String prompt) {
        GeminiConfig config = getGeminiConfig(traceId);
        return invokeGemini(traceId, prompt, config.apiUrl(), config.apiKey(), "pro-fallback");
    }

    private ContentGenerateResponseDTO parseStructuredContentResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new ContentGenerateResponseDTO("", "");
        }

        try {
            JsonNode jsonResult = mapper.readTree(extractJsonObject(rawResponse));
            if (jsonResult.has("common") || jsonResult.has("xOnly")) {
                return new ContentGenerateResponseDTO(
                        jsonResult.path("common").asText(""),
                        jsonResult.path("xOnly").asText(""),
                        jsonResult.path("xHashtags").asText("")
                );
            }
        } catch (Exception ignored) {
            // Fall through to plain-text adaptation if the model did not return valid JSON.
        }

        String commonText = rawResponse.trim();
        String xText = commonText.length() > 200 ? commonText.substring(0, 200).trim() : commonText;
        return new ContentGenerateResponseDTO(commonText, xText);
    }

    private String extractJsonObject(String rawResponse) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group();
        }
        return rawResponse;
    }

    public String generateTextFlash(String traceId, String prompt) {
        GeminiConfig config = getGeminiConfig(traceId);
        String flashUrl = Optional.ofNullable(geminiFlashApiUrlOverride)
                .filter(value -> !value.isBlank())
                .orElse("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
        return invokeGemini(traceId, prompt, flashUrl, config.apiKey(), "flash").getGeneratedText();
    }

    /**
     * Calls Gemini Flash with Google Search grounding enabled. The model uses live web results to ground its answer, so prompts that depend on
     * current events (e.g. trending topics) return up-to-date content instead of relying on the model's training cutoff.
     *
     * Grounded responses are incompatible with {@code generationConfig.response_mime_type=application/json}, so this path returns plain text.
     */
    public String generateTextFlashGrounded(String traceId, String prompt) {
        log.info("[{}]|GEMINI_CLIENT|Start grounded AI call with prompt", traceId);
        GeminiConfig config = getGeminiConfig(traceId);
        String flashUrl = Optional.ofNullable(geminiFlashApiUrlOverride)
                .filter(value -> !value.isBlank())
                .orElse("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");

        HttpEntity<Map<String, Object>> request = buildGroundedRequest(prompt, config.apiKey());
        log.info("[{}]|GEMINI_CLIENT|Calling Gemini flash model API (grounded): {}", traceId, flashUrl);
        ResponseEntity<String> response = restTemplate.exchange(flashUrl, HttpMethod.POST, request, String.class);
        log.info("[{}]|GEMINI_CLIENT|Gemini flash (grounded) response status: {}", traceId, response.getStatusCode());

        return extractTextFromResponse(traceId, response.getBody(), "flash-grounded");
    }

    private ContentGenerateResponseDTO invokeGemini(String traceId, String prompt, String apiUrl, String apiKey, String modelLabel) {
        HttpEntity<Map<String, Object>> request = buildRequest(prompt, apiKey);

        log.info("[{}]|GEMINI_CLIENT|Calling Gemini {} model API: {}", traceId, modelLabel, apiUrl);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);
        log.info("[{}]|GEMINI_CLIENT|Gemini {} response status: {}", traceId, modelLabel, response.getStatusCode());

        String geminiTextResponse = extractTextFromResponse(traceId, response.getBody(), modelLabel);
        return parseStructuredContentResponse(geminiTextResponse);
    }

    private HttpEntity<Map<String, Object>> buildRequest(String prompt, String apiKey) {
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart));
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", apiKey);

        return new HttpEntity<>(requestBody, headers);
    }

    private HttpEntity<Map<String, Object>> buildGroundedRequest(String prompt, String apiKey) {
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart));
        requestBody.put("contents", List.of(content));

        // Enable Google Search grounding so Gemini can ground answers in current
        // web results. Note: grounding is incompatible with
        // generationConfig.response_mime_type=application/json, so this request
        // intentionally omits the JSON mime type and returns plain text.
        Map<String, Object> googleSearchTool = new HashMap<>();
        googleSearchTool.put("google_search", new HashMap<>());
        requestBody.put("tools", List.of(googleSearchTool));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-goog-api-key", apiKey);

        return new HttpEntity<>(requestBody, headers);
    }

    private String extractTextFromResponse(String traceId, String responseBody, String modelLabel) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("Gemini " + modelLabel + " response was empty");
        }

        JsonNode root;
        try {
            root = mapper.readTree(responseBody);
        }
        catch (Exception ex) {
            throw new RuntimeException("Gemini " + modelLabel + " response parsing failed", ex);
        }
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            JsonNode promptFeedback = root.path("promptFeedback");
            String finishReason = promptFeedback.isMissingNode() ? "unknown" : promptFeedback.toString();
            throw new RuntimeException("Gemini " + modelLabel + " response contained no candidates: " + finishReason);
        }

        JsonNode textNode = candidates.path(0).path("content").path("parts").path(0).path("text");
        String text = textNode.asText("");
        if (text.isBlank()) {
            throw new RuntimeException("Gemini " + modelLabel + " response contained no generated text");
        }

        log.debug("[{}]|GEMINI_CLIENT|Gemini {} raw response: {}", traceId, modelLabel, responseBody);
        return text;
    }

    private GeminiConfig getGeminiConfig(String traceId) {
        String apiKey = getConfig(traceId, ConfigKeys.GEMINI_API_KEY, geminiApiKeyOverride);
        String apiUrl = getConfig(traceId, ConfigKeys.GEMINI_API_URL, geminiApiUrlOverride);

        if (apiKey == null || apiKey.isBlank() || apiUrl == null || apiUrl.isBlank()) {
            log.error("[{}]|GEMINI_CLIENT|Config missing|API key configured:{}|API url configured:{}",
                    traceId, apiKey != null && !apiKey.isBlank(), apiUrl != null && !apiUrl.isBlank());
            throw new RuntimeException("Gemini configuration missing: APP_AI_GEMINI_API_KEY or APP_AI_GEMINI_API_URL is not configured");
        }

        return new GeminiConfig(apiKey, apiUrl);
    }

    private String getConfig(String traceId, String key, String overrideValue) {
        if (overrideValue != null && !overrideValue.isBlank()) {
            return overrideValue;
        }
        return appCache.getConfigParameterValue(traceId, key);
    }

    private boolean shouldFallbackToPro(HttpStatusCode statusCode) {
        int code = statusCode.value();
        return code == 429 || code == 503;
    }

    private String buildHttpErrorMessage(String prefix, HttpStatusCode statusCode, String responseBody) {
        String sanitizedBody = responseBody == null || responseBody.isBlank() ? "empty response body" : responseBody;
        return prefix + " (" + statusCode.value() + "): " + sanitizedBody;
    }

    private String buildUserFriendlyError(int statusCode, String responseBody) {
        String body = responseBody != null ? responseBody.toLowerCase() : "";
        if (statusCode == 429 || body.contains("quota") || body.contains("rate")) {
            return "AI generation limit reached. Please try again in a few minutes.";
        }
        if (statusCode == 503 || body.contains("overloaded") || body.contains("unavailable")) {
            return "AI service is temporarily unavailable. Please try again shortly.";
        }
        if (body.contains("safety") || body.contains("blocked")) {
            return "Content could not be generated due to safety filters. Please rephrase your topic.";
        }
        return "Content generation failed. Please try again.";
    }

    private record GeminiConfig(String apiKey, String apiUrl) {
    }
}
