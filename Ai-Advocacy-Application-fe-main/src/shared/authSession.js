const AUTH_TOKEN_KEY = "authToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const ORG_ID_KEY = "orgId";
const ROLE_KEY = "role";

const removeKey = (storage, key) => {
  try {
    storage.removeItem(key);
  } catch (error) {
    // Ignore storage access failures and continue best-effort cleanup.
  }
};

const setKey = (storage, key, value) => {
  try {
    storage.setItem(key, value);
  } catch (error) {
    // Ignore storage access failures and continue best-effort persistence.
  }
};

const getKey = (storage, key) => {
  try {
    return storage.getItem(key);
  } catch (error) {
    return null;
  }
};

const normalizeNumber = (value) => {
  if (value === null || value === undefined || value === "" || value === "null" || value === "undefined") {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

export const normalizeRoles = (roleValue) => {
  if (Array.isArray(roleValue)) {
    return roleValue.filter(Boolean);
  }

  if (!roleValue) {
    return [];
  }

  if (typeof roleValue === "string") {
    try {
      const parsed = JSON.parse(roleValue);
      return Array.isArray(parsed) ? parsed.filter(Boolean) : [parsed].filter(Boolean);
    } catch (error) {
      return [roleValue];
    }
  }

  return [roleValue].filter(Boolean);
};

export const getStoredAuthToken = () =>
  getKey(sessionStorage, AUTH_TOKEN_KEY) || getKey(localStorage, AUTH_TOKEN_KEY);

export const getStoredRefreshToken = () =>
  getKey(sessionStorage, REFRESH_TOKEN_KEY) || getKey(localStorage, REFRESH_TOKEN_KEY);

export const getStoredOrgId = () =>
  getKey(sessionStorage, ORG_ID_KEY) || getKey(localStorage, ORG_ID_KEY);

export const getStoredRoles = () =>
  normalizeRoles(getKey(sessionStorage, ROLE_KEY) || getKey(localStorage, ROLE_KEY));

export const hasStoredSessionToken = () => Boolean(getStoredAuthToken());

export const getStoredSession = () => {
  const token = getStoredAuthToken();
  const refreshToken = getStoredRefreshToken();
  const organizationId = normalizeNumber(getStoredOrgId());
  const role = getStoredRoles();

  return {
    token,
    refreshToken,
    organizationId,
    role,
    requiresOrganizationSetup: Boolean(token) && organizationId == null,
  };
};

export const persistStoredSession = (sessionOrToken) => {
  if (typeof sessionOrToken === "string") {
    setKey(sessionStorage, AUTH_TOKEN_KEY, sessionOrToken);
    removeKey(localStorage, AUTH_TOKEN_KEY);
    return;
  }

  const session = sessionOrToken || {};

  if (session.token) {
    setKey(sessionStorage, AUTH_TOKEN_KEY, session.token);
  } else {
    removeKey(sessionStorage, AUTH_TOKEN_KEY);
  }
  removeKey(localStorage, AUTH_TOKEN_KEY);

  if (session.refreshToken) {
    setKey(sessionStorage, REFRESH_TOKEN_KEY, session.refreshToken);
  } else {
    removeKey(sessionStorage, REFRESH_TOKEN_KEY);
    removeKey(localStorage, REFRESH_TOKEN_KEY);
  }

  if (session.organizationId != null) {
    const orgValue = String(session.organizationId);
    setKey(sessionStorage, ORG_ID_KEY, orgValue);
    setKey(localStorage, ORG_ID_KEY, orgValue);
  } else {
    removeKey(sessionStorage, ORG_ID_KEY);
    removeKey(localStorage, ORG_ID_KEY);
  }

  const roleValue = JSON.stringify(session.role || []);
  setKey(sessionStorage, ROLE_KEY, roleValue);
  removeKey(localStorage, ROLE_KEY);
};

export const updateStoredTokens = ({ accessToken, refreshToken }) => {
  if (accessToken) {
    setKey(sessionStorage, AUTH_TOKEN_KEY, accessToken);
    removeKey(localStorage, AUTH_TOKEN_KEY);
  }

  if (refreshToken) {
    setKey(sessionStorage, REFRESH_TOKEN_KEY, refreshToken);
  }
};

export const clearStoredSession = () => {
  [
    AUTH_TOKEN_KEY,
    REFRESH_TOKEN_KEY,
    ORG_ID_KEY,
    ROLE_KEY,
  ].forEach((key) => {
    removeKey(sessionStorage, key);
    removeKey(localStorage, key);
  });
};
