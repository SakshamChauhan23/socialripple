const TYPE_TITLES = {
  LOYALTY_POINT_ADDED: "Points earned",
};

const PLATFORM_LABELS = {
  LINKEDIN: "LinkedIn",
  FACEBOOK: "Facebook",
  INSTAGRAM: "Instagram",
  TWITTER: "Twitter",
  YOUTUBE: "YouTube",
};

const normalizePlatformLabels = (message = "") =>
  Object.entries(PLATFORM_LABELS).reduce(
    (nextMessage, [rawLabel, displayLabel]) =>
      nextMessage.replace(new RegExp(`\\b${rawLabel}\\b`, "g"), displayLabel),
    message
  );

const formatLoyaltyPointMessage = (message = "") => {
  let formattedMessage = normalizePlatformLabels(message);

  formattedMessage = formattedMessage.replace(
    /^Loyalty points added for /,
    "You earned points for "
  );
  formattedMessage = formattedMessage.replace(/sharing in /g, "sharing on ");
  formattedMessage = formattedMessage.replace(/posting in /g, "posting on ");
  formattedMessage = formattedMessage.replace(
    /Added points:\s*(\d+)/i,
    "+$1 points"
  );

  return formattedMessage || "You have a new update.";
};

export const formatNotification = (notification = {}) => {
  const { type, data } = notification;

  if (type === "LOYALTY_POINT_ADDED") {
    return {
      title: TYPE_TITLES[type],
      message: formatLoyaltyPointMessage(data),
    };
  }

  return {
    title: TYPE_TITLES[type] || "Notification",
    message: normalizePlatformLabels(data) || "You have a new update.",
  };
};
