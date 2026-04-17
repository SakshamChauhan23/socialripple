const trimTrailingSlash = (value) => value?.replace(/\/+$/, "") || "";

const buildBaseUrl = (baseUrl, suffix = "") => {
  const normalizedBase = trimTrailingSlash(baseUrl);
  return normalizedBase ? `${normalizedBase}${suffix}` : "";
};

const userApiRoot =
  process.env.REACT_APP_USER_API_ROOT ||
  process.env.REACT_APP_USER_API_BASE_URL ||
  process.env.REACT_APP_API_ROOT ||
  "";

const userApiBase =
  process.env.REACT_APP_USER_API_URL ||
  process.env.REACT_APP_USER_API_BASE_URL ||
  process.env.REACT_APP_API_URL ||
  buildBaseUrl(userApiRoot, "/v1/");

const platformApiRoot =
  process.env.REACT_APP_PLATFORM_API_ROOT ||
  process.env.REACT_APP_PLATFORM_API_BASE_URL ||
  process.env.REACT_APP_API_ROOT_EXTERNAL ||
  process.env.REACT_APP_API_URL_EXTERNAL ||
  userApiRoot;

const platformApiBase =
  process.env.REACT_APP_PLATFORM_API_URL ||
  process.env.REACT_APP_PLATFORM_API_BASE_URL ||
  process.env.REACT_APP_API_URL_EXTERNAL ||
  buildBaseUrl(platformApiRoot, "/");

const userWsBase =
  process.env.REACT_APP_USER_WS_URL ||
  process.env.REACT_APP_WS_URL ||
  "";

export { platformApiBase, platformApiRoot, userApiBase, userApiRoot, userWsBase };
