// src/msalConfig.js
export const msalConfig = {
  auth: {
    clientId:
      process.env.REACT_APP_MICROSOFT_CLIENT_ID || "",
    authority: `https://login.microsoftonline.com/${
      process.env.REACT_APP_MICROSOFT_TENANT_ID || ""
    }`,
    redirectUri:
      typeof window !== "undefined"
        ? `${window.location.origin}/sign-in`
        : "/sign-in",
  },
};
