package com.social.ripple.external_ingestion.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.util.enumeration.Platform;

public final class PlatformBusinessPageComposer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final int X_URL_LENGTH = 23;

    private PlatformBusinessPageComposer() {
    }

    public static String composeContent(String baseContent, String socialHandleConfig, Platform platform) {
        return composeContent(baseContent, socialHandleConfig, platform, null);
    }

    public static String composeContent(String baseContent, String socialHandleConfig, Platform platform, List<String> hashtags) {
        String normalizedBaseContent = baseContent == null ? "" : baseContent.trim();
        String suffix = resolveSuffix(socialHandleConfig, platform);
        String hashtagText = resolveHashtagText(hashtags);

        StringBuilder composed = new StringBuilder();
        appendSection(composed, normalizedBaseContent);
        if (StringUtils.hasText(suffix) && !normalizedBaseContent.contains(suffix)) {
            appendSection(composed, suffix);
        }
        if (StringUtils.hasText(hashtagText) && !normalizedBaseContent.contains(hashtagText)) {
            appendSection(composed, hashtagText);
        }

        return composed.toString();
    }

    public static void validateLength(String content, Platform platform) {
        int limit = switch (platform) {
            case X -> 280;
            case LINKEDIN -> 3000;
            case INSTAGRAM -> 2200;
            case FACEBOOK -> 63204;
        };

        int actualLength = getPlatformLength(content, platform);
        if (actualLength > limit) {
            throw new IllegalArgumentException(platform + " content exceeds the character limit of " + limit);
        }
    }

    private static String resolveSuffix(String socialHandleConfig, Platform platform) {
        JsonNode platformNode = getPlatformNode(socialHandleConfig, platform);
        if (platformNode == null || platformNode.isNull()) {
            return "";
        }

        String username = getCandidateField(platformNode, "username", "screenName", "handle", "userName");
        String url = getCandidateField(platformNode, "businessPageLink", "business_page_link", "businessLink", "business_link",
                "pageLink", "page_link", "pageUrl", "page_url", "profileUrl", "profile_url", "url", "link");

        return switch (platform) {
            case X, INSTAGRAM -> StringUtils.hasText(username) ? "@" + username : defaultString(url);
            case FACEBOOK, LINKEDIN -> StringUtils.hasText(url) ? "Follow Us: " + defaultString(url) : (StringUtils.hasText(username) ? "Follow Us: @" + username : "");
        };
    }

    private static JsonNode getPlatformNode(String socialHandleConfig, Platform platform) {
        if (!StringUtils.hasText(socialHandleConfig)) {
            return null;
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(socialHandleConfig);
            if (rootNode == null || rootNode.isNull()) {
                return null;
            }
            if (platform == Platform.X) {
                JsonNode xNode = rootNode.get("x");
                if (xNode != null && !xNode.isNull()) {
                    return xNode;
                }
                return rootNode.get("twitter");
            }
            return rootNode.get(platform.name().toLowerCase());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getCandidateField(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return "";
        }
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                String value = fieldNode.asText("");
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private static String resolveHashtagText(List<String> hashtags) {
        if (hashtags == null || hashtags.isEmpty()) {
            return "";
        }

        return hashtags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static void appendSection(StringBuilder composed, String section) {
        if (!StringUtils.hasText(section)) {
            return;
        }

        if (composed.length() > 0) {
            composed.append("\n");
        }
        composed.append(section.trim());
    }

    private static int getPlatformLength(String value, Platform platform) {
        String content = value == null ? "" : value;
        if (platform != Platform.X) {
            return content.codePointCount(0, content.length());
        }

        int total = 0;
        int lastIndex = 0;
        Matcher matcher = URL_PATTERN.matcher(content);
        while (matcher.find()) {
            total += content.substring(lastIndex, matcher.start()).codePointCount(0, matcher.start() - lastIndex);
            total += X_URL_LENGTH;
            lastIndex = matcher.end();
        }
        total += content.substring(lastIndex).codePointCount(0, content.length() - lastIndex);
        return total;
    }
}
