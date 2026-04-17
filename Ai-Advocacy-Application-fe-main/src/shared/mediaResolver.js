const BUNNY_PLAYBACK_SEGMENT = "/play/";

export const isVideoMedia = (media = {}) =>
  (media?.mediaType || "").toUpperCase() === "VIDEO";

export const isImageMedia = (media = {}) =>
  (media?.mediaType || "").toUpperCase() === "IMAGE";

export const isBunnyStreamMedia = (media = {}) => {
  const storageProvider = (media?.storageProvider || "").toUpperCase();
  const playbackUrl = media?.playbackUrl || "";
  return (
    storageProvider === "BUNNY_STREAM" ||
    playbackUrl.includes("video.bunnycdn.com") ||
    playbackUrl.includes(BUNNY_PLAYBACK_SEGMENT)
  );
};

export const getResolvedImageUrl = (media = {}, fallback = "") =>
  media?.fileUrl || media?.thumbnailUrl || fallback;

export const getResolvedPosterUrl = (media = {}, fallback = "") =>
  media?.thumbnailUrl || media?.fileUrl || fallback;

export const getResolvedVideoUrl = (media = {}) =>
  media?.playbackUrl || media?.fileUrl || "";

export const getBunnyEmbedUrl = (playbackUrl = "") => {
  try {
    const url = new URL(playbackUrl);
    const [, playSegment, libraryId, videoGuid] = url.pathname.split("/");
    if (playSegment !== "play" || !libraryId || !videoGuid) {
      return playbackUrl;
    }
    return `https://iframe.mediadelivery.net/embed/${libraryId}/${videoGuid}`;
  } catch (error) {
    return playbackUrl;
  }
};
