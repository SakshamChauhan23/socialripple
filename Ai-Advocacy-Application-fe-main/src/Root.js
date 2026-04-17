import { GoogleOAuthProvider } from "@react-oauth/google";
import { MsalProvider } from "@azure/msal-react";
import { PublicClientApplication } from "@azure/msal-browser";
import App from "./App";
import { GoogleReCaptchaProvider } from "react-google-recaptcha-v3";

const MICROSOFT_TENANT_ID =
  process.env.REACT_APP_MICROSOFT_TENANT_ID || "";
const MICROSOFT_CLIENT_ID =
  process.env.REACT_APP_MICROSOFT_CLIENT_ID || "";
const GOOGLE_SSO_CLIENT_ID =
  process.env.REACT_APP_GOOGLE_SSO_CLIENT_ID || "";
const redirectUri =
  typeof window !== "undefined"
    ? `${window.location.origin}/sign-in`
    : "/sign-in";

const msalInstance = new PublicClientApplication({
  auth: {
    clientId: MICROSOFT_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${MICROSOFT_TENANT_ID}`,
    redirectUri,
  },
});

const RECAPTCHA_SITE_KEY =
  process.env.REACT_APP_RECAPTCHA_SITE_KEY || "";

const Root = () => (
  <GoogleReCaptchaProvider reCaptchaKey={RECAPTCHA_SITE_KEY}>
    <GoogleOAuthProvider clientId={GOOGLE_SSO_CLIENT_ID}>
      <MsalProvider instance={msalInstance}>
        <App />
      </MsalProvider>
    </GoogleOAuthProvider>
  </GoogleReCaptchaProvider>
);

export default Root;
