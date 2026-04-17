import { toast } from "react-toastify";
import apiClient from "./apiClient";
import authClient from "./authClient";
import { getStoredRefreshToken } from "../shared/authSession";

const normalizeOrganizationId = (value) => {
  if (value === null || value === undefined || value === "" || value === "null" || value === "undefined") {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const normalizeRoles = (roleValue) => {
  if (Array.isArray(roleValue)) {
    return roleValue.filter(Boolean);
  }

  if (typeof roleValue === "string" && roleValue) {
    try {
      const parsed = JSON.parse(roleValue);
      return Array.isArray(parsed) ? parsed.filter(Boolean) : [parsed].filter(Boolean);
    } catch (error) {
      return [roleValue];
    }
  }

  return [];
};

const buildSessionFromAuthPayload = (authPayload = {}) => {
  const token = authPayload?.accessToken || authPayload?.token || null;
  const refreshToken = authPayload?.refreshToken || null;
  const organizationId = normalizeOrganizationId(authPayload?.organizationId);
  const role = normalizeRoles(authPayload?.role);

  return {
    token,
    refreshToken,
    organizationId,
    role,
    requiresOrganizationSetup: Boolean(token) && organizationId == null,
  };
};

const authService = {
  signIn: async (email, password, captchaToken) => {
    try {
      return await authClient.post("auth/login", {
        userName: email,
        password,
        captchaToken,
      });
    } catch (error) {
      console.error("Sign In error", error.response?.data || error.message);
      throw error;
    }
  },
  register: async (formData) => {
    try {
      return await apiClient.post("auth/register", formData);
    } catch (error) {
      console.error("Register Error:", error.response?.data || error.message);
      throw error;
    }
  },
  signupWithSso: async (payload) => {
    try {
      return await apiClient.post("auth/sso/signup", payload);
    } catch (error) {
      console.error("Some-thing error", error.response?.data || error.message);
      throw error;
    }
  },
  registerWithSso: async (payload) => {
    try {
      return await apiClient.post("auth/sso/register", payload);
    } catch (error) {
      console.error("Some-thing error", error.response?.data || error.message);
      throw error;
    }
  },
  loginWithSso: async (payload) => {
    try {
      return await apiClient.post("auth/sso/signin", payload);
    } catch (error) {
      console.error("Some-thing error", error.response?.data || error.message);
      throw error;
    }
  },
  signupWithGoogle: async (payload) => authService.signupWithSso(payload),
  loginWithGoogle: async (payload) => authService.loginWithSso(payload),

  getInviteInfo: async (token) => {
    try {
      return await apiClient.get(`auth/invite-info?token=${encodeURIComponent(token)}`);
    } catch (error) {
      console.error("Invite info error:", error.response?.data || error.message);
      return null;
    }
  },

  signUp: async (formData) => {
    try {
      return await apiClient.post("auth/signup", formData);
    } catch (error) {
      console.error("Some-thing Error:", error.response?.data || error.message);
      throw error;
    }
  },
  completeOrganizationOnboarding: async (payload) => {
    try {
      return await apiClient.post("organizations/onboarding", payload);
    } catch (error) {
      console.error("Onboarding Error:", error.response?.data || error.message);
      throw error;
    }
  },
  refreshToken: async () => {
    try {
      const refreshToken = getStoredRefreshToken();
      const payload = {
        refreshToken: refreshToken,
      };
      const response = await apiClient.post("auth/token/refresh", payload);
      return response;
    } catch (error) {
      console.error("Some-thing Error:", error.response?.data || error.message);
      throw error;
    }
  },

  passwordRecovery: (email) =>
    apiClient.post("auth/password/forgot", { email }),

  updatePassword: async (formData) => {
    try {
      const response = await apiClient.post("auth/reset-password", formData);
      return response;
    } catch (error) {
      console.error("Some-thing Error:", error.response?.data || error.message);
      toast.error(error.response?.data?.message || "Failed to update password");
      throw error;
    }
  },
  forgotPassword: async (formData) => {
    try {
      const response = await apiClient.post("auth/password/forgot", formData);
      return response;
    } catch (error) {
      console.error("Some-thing Error:", error.response?.data || error.message);
      throw error;
    }
  },
  extractSession: (response) => {
    if (response?.data?.userData) {
      return buildSessionFromAuthPayload(response.data.userData);
    }

    if (response?.data?.authResponse) {
      return buildSessionFromAuthPayload(response.data.authResponse);
    }

    if (response?.data) {
      return buildSessionFromAuthPayload(response.data);
    }

    return buildSessionFromAuthPayload();
  },
};

export default authService;
