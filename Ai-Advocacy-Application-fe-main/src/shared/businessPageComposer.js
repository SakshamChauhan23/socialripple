const PLATFORM_LIMITS = {
  x: 280,
  linkedin: 3000,
  instagram: 2200,
  facebook: 63204,
};

const X_URL_LENGTH = 23;
const URL_REGEX = /(https?:\/\/[^\s]+)/gi;

export const normalizePlatformKey = (platformName = "") => {
  const normalized = platformName.toLowerCase().trim();
  if (normalized === "twitter/x" || normalized === "twitter / x" || normalized === "x") {
    return "x";
  }
  if (normalized === "linkedin") {
    return "linkedin";
  }
  if (normalized === "facebook") {
    return "facebook";
  }
  if (normalized === "instagram") {
    return "instagram";
  }
  return normalized;
};

const getCharacterCount = (value) => Array.from(value || "").length;

const getPlatformLength = (value, platformKey) => {
  const text = value || "";
  if (platformKey !== "x") {
    return getCharacterCount(text);
  }

  let total = 0;
  let lastIndex = 0;
  const matches = text.matchAll(URL_REGEX);
  for (const match of matches) {
    const matchedUrl = match[0];
    const matchIndex = match.index || 0;
    total += getCharacterCount(text.slice(lastIndex, matchIndex));
    total += X_URL_LENGTH;
    lastIndex = matchIndex + matchedUrl.length;
  }
  total += getCharacterCount(text.slice(lastIndex));
  return total;
};

export const getBusinessPageEntry = (businessPages = {}, platformName = "") =>
  businessPages?.[normalizePlatformKey(platformName)] || {};

export const getPlatformLimit = (platformName = "") =>
  PLATFORM_LIMITS[normalizePlatformKey(platformName)] || 0;

export const getPlatformSuffix = (platformName = "", businessPages = {}) => {
  const businessPage = getBusinessPageEntry(businessPages, platformName);
  if (businessPage?.tagText) {
    return {
      mode: "tag",
      text: businessPage.tagText,
    };
  }
  if (businessPage?.url) {
    return {
      mode: "url",
      text: businessPage.url,
    };
  }
  return {
    mode: "none",
    text: "",
  };
};

const getHashtagText = (hashtags = []) =>
  (Array.isArray(hashtags) ? hashtags : [])
    .filter((tag) => typeof tag === "string" && tag.trim())
    .map((tag) => tag.trim())
    .join(", ");

const appendSection = (sections = [], section = "") => {
  if (section) {
    sections.push(section);
  }
  return sections;
};

export const composePlatformContent = (
  content = "",
  platformName = "",
  businessPages = {},
  hashtags = []
) => {
  const platformKey = normalizePlatformKey(platformName);
  const suffix = getPlatformSuffix(platformName, businessPages);
  const hashtagText = getHashtagText(hashtags);
  const baseContent = content || "";
  const reservedSections = [];
  if (suffix.text) {
    appendSection(reservedSections, suffix.text);
  }
  if (hashtagText && !baseContent.includes(hashtagText)) {
    appendSection(reservedSections, hashtagText);
  }
  const reservedText = reservedSections.join("\n\n");
  const reservedLength = reservedText
    ? getPlatformLength(`${baseContent ? "\n\n" : ""}${reservedText}`, platformKey)
    : 0;
  const limit = getPlatformLimit(platformName);
  const maxBaseCharacters = limit ? Math.max(limit - reservedLength, 0) : 0;
  const baseLength = getPlatformLength(baseContent, platformKey);

  const finalSections = [];
  appendSection(finalSections, baseContent);
  if (suffix.text && !baseContent.includes(suffix.text)) {
    appendSection(finalSections, suffix.text);
  }
  if (hashtagText && !baseContent.includes(hashtagText)) {
    appendSection(finalSections, hashtagText);
  }

  const finalText = finalSections.join("\n\n");

  const effectiveLength = getPlatformLength(finalText, platformKey);

  return {
    platformKey,
    limit,
    suffixMode: suffix.mode,
    suffixText: suffix.text,
    finalText,
    baseLength,
    reservedLength,
    effectiveLength,
    maxBaseCharacters,
    remainingCharacters: Math.max(maxBaseCharacters - baseLength, 0),
    isOverLimit: limit ? effectiveLength > limit : false,
    isSuffixTooLong: suffix.text ? reservedLength > limit : false,
  };
};

export const clampPlatformContent = (
  nextValue = "",
  platformName = "",
  businessPages = {},
  hashtags = []
) => {
  const composition = composePlatformContent(nextValue, platformName, businessPages, hashtags);
  if (!composition.limit || composition.baseLength <= composition.maxBaseCharacters) {
    return nextValue;
  }

  const characters = Array.from(nextValue);
  while (characters.length > 0) {
    characters.pop();
    const candidate = characters.join("");
    const candidateComposition = composePlatformContent(candidate, platformName, businessPages, hashtags);
    if (candidateComposition.baseLength <= candidateComposition.maxBaseCharacters) {
      return candidate;
    }
  }

  return "";
};
