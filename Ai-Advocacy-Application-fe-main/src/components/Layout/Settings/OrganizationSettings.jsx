import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {
  connectBusinessPage,
  deleteBusinessPage,
  getAvailableBusinessPages,
  getBusinessPages,
  getLeaderPages,
  saveBusinessPageManual,
  selectBusinessPage,
} from "../../../services/settingsService";
import { hasStoredSessionToken } from "../../../shared/authSession";

const PLATFORM_ORDER = ["FACEBOOK", "INSTAGRAM", "LINKEDIN", "X"];

const PLATFORM_LABELS = {
  FACEBOOK: "Facebook",
  INSTAGRAM: "Instagram",
  LINKEDIN: "LinkedIn",
  X: "X",
};

const PLATFORM_OAUTH_DEFAULTS = {
  FACEBOOK: {
    oauthSupported: true,
    manualFallbackAvailable: false,
    oauthProviderLabel: "Meta",
    supportMessage:
      "Connect only organization-owned Facebook Pages through the shared Meta app. Personal Facebook profiles are not allowed.",
  },
  INSTAGRAM: {
    oauthSupported: true,
    manualFallbackAvailable: false,
    oauthProviderLabel: "Meta",
    supportMessage:
      "Connect only Instagram business or professional accounts linked to a Facebook Page. Personal Instagram accounts are not allowed.",
  },
  LINKEDIN: {
    oauthSupported: true,
    manualFallbackAvailable: true,
    oauthProviderLabel: "LinkedIn",
    supportMessage:
      "Connect your organization's LinkedIn company page. You must be an admin of the LinkedIn company page to connect it. Manual setup is also available as a fallback.",
  },
  X: {
    oauthSupported: true,
    manualFallbackAvailable: false,
    oauthProviderLabel: "X",
    supportMessage:
      "Connect the organization-owned X handle through the shared X app. After OAuth, an admin must confirm the connected handle before it is saved as the organization business account.",
  },
};

const accordionSx = {
  background: "#f5f5f5",
  borderRadius: "8px !important",
  boxShadow: "none",
  "&::before": { display: "none" },
};

const accordionSummarySx = {
  padding: 0,
  pr: 2,
  "& .MuiAccordionSummary-content": {
    margin: 0,
  },
};

const accordionDetailsSx = {
  background: "#ffffff8f",
  borderRadius: "8px",
  mt: 1,
  p: 2,
};

const emptyPlatformState = (platform, capabilityKnown = false) => ({
  platform,
  connected: false,
  enabled: true,
  oauthSupported: capabilityKnown
    ? PLATFORM_OAUTH_DEFAULTS[platform]?.oauthSupported ?? false
    : null,
  manualFallbackAvailable: capabilityKnown
    ? PLATFORM_OAUTH_DEFAULTS[platform]?.manualFallbackAvailable ?? true
    : null,
  oauthProviderLabel: capabilityKnown
    ? PLATFORM_OAUTH_DEFAULTS[platform]?.oauthProviderLabel ?? ""
    : "",
  supportMessage: capabilityKnown
    ? PLATFORM_OAUTH_DEFAULTS[platform]?.supportMessage ?? ""
    : "",
  apiUrl: "",
  accessToken: "",
  accessTokenPreview: "",
  accessTokenConfigured: false,
  refreshToken: "",
  refreshTokenPreview: "",
  refreshTokenConfigured: false,
  externalUserId: "",
  pageId: "",
  username: "",
  pageUrl: "",
  businessPageLink: "",
  displayName: "",
  lastSyncAttemptAt: "",
  lastSyncSuccessAt: "",
  lastSyncStatus: "NEVER_SYNCED",
  lastSyncError: "",
  lastSyncErrorAt: "",
  lastImportedCount: null,
  scheduleCadenceLabel: "",
  nextScheduledFetchAt: "",
});

const formatSyncDateTime = (value) => {
  if (!value) {
    return "Never";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString();
};

const getSyncStatusMeta = (page) => {
  if (!page.connected) {
    return {
      label: "Disconnected",
      color: "default",
      variant: "outlined",
    };
  }

  switch (page.lastSyncStatus) {
    case "IN_PROGRESS":
      return { label: "Syncing", color: "info", variant: "filled" };
    case "SUCCESS_WITH_NEW_CONTENT":
      return { label: "Synced", color: "success", variant: "filled" };
    case "SUCCESS_NO_NEW_CONTENT":
      return { label: "No New Content", color: "success", variant: "outlined" };
    case "SUCCESS":
      return { label: "Healthy", color: "success", variant: "filled" };
    case "FAILED":
      return { label: "Failed", color: "error", variant: "filled" };
    default:
      return { label: "Never Synced", color: "warning", variant: "outlined" };
  }
};

const getLeaderSyncStatusMeta = (platformStatus) => {
  const connected = platformStatus?.status === "CONNECTED";
  if (!connected) {
    return {
      label: "Disconnected",
      color: "default",
      variant: "outlined",
    };
  }

  switch (platformStatus?.lastSyncStatus) {
    case "IN_PROGRESS":
      return { label: "Syncing", color: "info", variant: "filled" };
    case "SUCCESS_WITH_NEW_CONTENT":
      return { label: "Synced", color: "success", variant: "filled" };
    case "SUCCESS_NO_NEW_CONTENT":
      return { label: "No New Content", color: "success", variant: "outlined" };
    case "SUCCESS":
      return { label: "Healthy", color: "success", variant: "filled" };
    case "FAILED":
      return { label: "Failed", color: "error", variant: "filled" };
    default:
      return { label: "Never Synced", color: "warning", variant: "outlined" };
  }
};

const getBusinessConnectErrorMessage = (platform, reason) => {
  const label = PLATFORM_LABELS[platform] || platform || "Business page";
  switch (reason) {
    case "transaction_expired":
      return `${label} connection expired. Start the business-page connection again.`;
    case "invalid_transaction":
      return `${label} connection could not be verified. Start the business-page connection again.`;
    case "token_exchange_failed":
      return `${label} connection failed while exchanging the provider token.`;
    case "page_discovery_failed":
      return `${label} connection failed while loading organization business pages.`;
    case "no_business_pages_found":
      return `${label} connection did not return any eligible organization business pages. Personal accounts are not allowed here.`;
    case "personal_account_not_allowed":
      return `${label} personal accounts are not allowed in business-page settings. Connect an organization-owned business page instead.`;
    case "manual_setup_required":
      return `${label} company pages must be connected manually using a valid organization access token and the numeric LinkedIn organization ID.`;
    case "unsupported_platform":
      return `${label} business-page connection is not available in this admin flow.`;
    default:
      return `${label} business page connection failed.`;
  }
};

const normalizePlatformCapabilities = (platform, item = {}, capabilityKnown = false) => {
  const defaults = PLATFORM_OAUTH_DEFAULTS[platform] || {};
  const normalized = {
    ...emptyPlatformState(platform, capabilityKnown),
    ...item,
  };

  if (capabilityKnown) {
    if (typeof normalized.oauthSupported !== "boolean") {
      normalized.oauthSupported = defaults.oauthSupported ?? false;
    }
    if (typeof normalized.manualFallbackAvailable !== "boolean") {
      normalized.manualFallbackAvailable =
        defaults.manualFallbackAvailable ?? true;
    }
    if (!normalized.oauthProviderLabel) {
      normalized.oauthProviderLabel = defaults.oauthProviderLabel ?? "";
    }
    if (!normalized.supportMessage) {
      normalized.supportMessage = defaults.supportMessage ?? "";
    }
  }

  return normalized;
};

const normalizedBusinessPages = (response, capabilityKnown = false) => {
  const source = Array.isArray(response?.businessPages)
    ? response.businessPages
    : [];
  const map = new Map(
    source.map((item) => [
      item.platform,
      normalizePlatformCapabilities(item.platform, item, capabilityKnown),
    ])
  );
  return PLATFORM_ORDER.map(
    (platform) =>
      map.get(platform) || normalizePlatformCapabilities(platform, {}, capabilityKnown)
  );
};

const normalizeSelectionOptions = (response) => {
  const source = Array.isArray(response?.availablePages)
    ? response.availablePages
    : Array.isArray(response?.pages)
      ? response.pages
      : Array.isArray(response?.options)
        ? response.options
        : [];

  return source
    .map((item) => {
      const selectionId =
        item?.selectionId ||
        item?.id ||
        item?.externalUserId ||
        item?.pageId ||
        item?.username;
      if (!selectionId) {
        return null;
      }

      return {
        selectionId: String(selectionId),
        displayName:
          item?.displayName || item?.name || item?.username || "X Account",
        username: item?.username || "",
        pageUrl: item?.pageUrl || item?.businessPageLink || "",
        externalUserId: item?.externalUserId || item?.pageId || "",
      };
    })
    .filter(Boolean);
};

const OrganizationSettings = () => {
  const navigate = useNavigate();
  const [businessPages, setBusinessPages] = useState(() =>
    normalizedBusinessPages(undefined, false)
  );
  const [leaders, setLeaders] = useState([]);
  const initParams = new URLSearchParams(window.location.search);
  const initStatus = initParams.get("businessConnect");
  const initPlatform = initParams.get("platform")?.toUpperCase();
  const initTransactionId = initParams.get("transactionId");
  const isLinkedInSelect = initStatus === "select" && initPlatform === "LINKEDIN" && initTransactionId;

  const [activePlatform, setActivePlatform] = useState(
    isLinkedInSelect ? "LINKEDIN" : (initPlatform === "X" && initStatus === "select" ? "X" : PLATFORM_ORDER[0])
  );
  const [loading, setLoading] = useState(true);
  const [deletingPlatform, setDeletingPlatform] = useState("");
  const [connectingPlatform, setConnectingPlatform] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [leaderErrorMessage, setLeaderErrorMessage] = useState("");
  const [forbidden, setForbidden] = useState(false);
  const [businessExpanded, setBusinessExpanded] = useState(!!isLinkedInSelect);
  const [leadersExpanded, setLeadersExpanded] = useState(false);
  const [savingManualPlatform, setSavingManualPlatform] = useState("");
  const [selectionTransactionId, setSelectionTransactionId] = useState(isLinkedInSelect ? initTransactionId : "");
  const [selectionOptions, setSelectionOptions] = useState([]);
  const [selectedOptionId, setSelectedOptionId] = useState("");
  const [selectionLoading, setSelectionLoading] = useState(false);
  const [selectingPlatform, setSelectingPlatform] = useState("");
  const [manualLinkedInForm, setManualLinkedInForm] = useState({
    accessToken: "",
    externalUserId: "",
    displayName: "",
    pageUrl: "",
    username: "",
  });

  useEffect(() => {
    let mounted = true;

    const loadSettings = async () => {
      setLoading(true);
      setErrorMessage("");
      setLeaderErrorMessage("");
      try {
        const [businessResult, leaderResult] = await Promise.allSettled([
          getBusinessPages(),
          getLeaderPages(),
        ]);

        if (!mounted) {
          return;
        }

        if (businessResult.status === "fulfilled") {
          setBusinessPages(normalizedBusinessPages(businessResult.value, true));
        } else {
          const businessError = businessResult.reason;
          if (businessError?.response?.status === 403) {
            setForbidden(true);
            return;
          }
          setErrorMessage("Failed to load business page settings.");
        }

        if (leaderResult.status === "fulfilled") {
          const leaderResponse = leaderResult.value;
          setLeaders(
            Array.isArray(leaderResponse?.leaderPages)
              ? leaderResponse.leaderPages
              : Array.isArray(leaderResponse?.leaders)
                ? leaderResponse.leaders
                : []
          );
        } else {
          const leaderError = leaderResult.reason;
          if (leaderError?.response?.status === 403) {
            setForbidden(true);
            return;
          }
          setLeaders([]);
          setLeaderErrorMessage(
            leaderError?.response?.data?.message ||
              "Unable to load leader status for this organization."
          );
        }
        setForbidden(false);
      } catch (error) {
        if (!mounted) {
          return;
        }
        if (error?.response?.status === 403) {
          setForbidden(true);
        } else {
          setErrorMessage("Failed to load business page settings.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadSettings();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const status = params.get("businessConnect");
    const platform = params.get("platform");
    const reason = params.get("reason");
    const transactionId = params.get("transactionId");
    if (!status) {
      return;
    }

    const normalizedPlatform = platform?.toUpperCase();
    const label = normalizedPlatform
      ? PLATFORM_LABELS[normalizedPlatform] || platform
      : "Business page";
    if (status === "success") {
      setSuccessMessage(`${label} business page connected successfully.`);
      setErrorMessage("");
    } else if (status === "error") {
      setErrorMessage(
        getBusinessConnectErrorMessage(normalizedPlatform, reason)
      );
      setSuccessMessage("");
    } else if (status === "select") {
      if ((normalizedPlatform === "X" || normalizedPlatform === "LINKEDIN") && transactionId) {
        setActivePlatform(normalizedPlatform);
        setBusinessExpanded(true);
        setSelectionTransactionId(transactionId);
        setSelectionOptions([]);
        setSelectedOptionId("");
        setErrorMessage("");
        setSuccessMessage(
          normalizedPlatform === "LINKEDIN"
            ? "Select the LinkedIn company page to connect as your organization business page."
            : "Confirm the connected X handle to save it as the organization business account."
        );
      } else {
        setErrorMessage(
          `${label} selection is not available in this admin flow.`
        );
        setSuccessMessage("");
      }
    }

    params.delete("businessConnect");
    params.delete("platform");
    params.delete("reason");
    if (status !== "select" || (normalizedPlatform !== "X" && normalizedPlatform !== "LINKEDIN")) {
      params.delete("transactionId");
    }
    const query = params.toString();
    const nextUrl = `${window.location.pathname}${
      query ? `?${query}` : ""
    }${window.location.hash || ""}`;
    window.history.replaceState({}, document.title, nextUrl);
  }, []);

  useEffect(() => {
    if (!selectionTransactionId || (activePlatform !== "X" && activePlatform !== "LINKEDIN")) {
      return;
    }

    let mounted = true;

    const loadSelectionOptions = async () => {
      setSelectionLoading(true);
      try {
        const response = await getAvailableBusinessPages(activePlatform, selectionTransactionId);
        if (!mounted) {
          return;
        }

        const options = normalizeSelectionOptions(response);
        setSelectionOptions(options);
        setSelectedOptionId((current) =>
          current && options.some((option) => option.selectionId === current)
            ? current
            : options[0]?.selectionId || ""
        );

        if (!options.length) {
          setErrorMessage(
            `${PLATFORM_LABELS[activePlatform] || activePlatform} connection completed, but no business pages were returned for this account.`
          );
        }
      } catch (error) {
        if (!mounted) {
          return;
        }
        setErrorMessage(
          error?.response?.data?.message ||
            `Failed to load available ${PLATFORM_LABELS[activePlatform] || activePlatform} business pages.`
        );
      } finally {
        if (mounted) {
          setSelectionLoading(false);
        }
      }
    };

    loadSelectionOptions();
    return () => {
      mounted = false;
    };
  }, [activePlatform, selectionTransactionId]);

  useEffect(() => {
    if (activePlatform === "X" || activePlatform === "LINKEDIN") {
      return;
    }
    setSelectionTransactionId("");
    setSelectionOptions([]);
    setSelectedOptionId("");
    setSelectionLoading(false);
    setSelectingPlatform("");
  }, [activePlatform]);

  const handleDisconnect = async (platform) => {
    setDeletingPlatform(platform);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      await deleteBusinessPage(platform);
      setBusinessPages((current) =>
        current.map((page) =>
          page.platform === platform ? emptyPlatformState(platform) : page
        )
      );
      setSuccessMessage(
        `${PLATFORM_LABELS[platform]} business page disconnected.`
      );
    } catch (error) {
      setErrorMessage(
        error?.response?.data?.message ||
          `Failed to disconnect ${PLATFORM_LABELS[platform]}.`
      );
    } finally {
      setDeletingPlatform("");
    }
  };

  const handleConnect = async (platform) => {
    if (!hasStoredSessionToken()) {
      setErrorMessage("Your session has expired. Please sign in again to reconnect this business page.");
      setSuccessMessage("");
      navigate("/sign-in");
      return;
    }

    setConnectingPlatform(platform);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const response = await connectBusinessPage(platform);
      if (response?.authUrl) {
        window.location.href = response.authUrl;
        return;
      }
      setErrorMessage(
        `No authorization URL was returned for ${PLATFORM_LABELS[platform]}.`
      );
    } catch (error) {
      setErrorMessage(
        error?.response?.data?.message ||
          `Failed to start ${PLATFORM_LABELS[platform]} connection.`
      );
    } finally {
      setConnectingPlatform("");
    }
  };

  const clearSelectionFlow = () => {
    setSelectionTransactionId("");
    setSelectionOptions([]);
    setSelectedOptionId("");
    setSelectionLoading(false);
    setSelectingPlatform("");

    const params = new URLSearchParams(window.location.search);
    params.delete("transactionId");
    const query = params.toString();
    const nextUrl = `${window.location.pathname}${
      query ? `?${query}` : ""
    }${window.location.hash || ""}`;
    window.history.replaceState({}, document.title, nextUrl);
  };

  const handleSelectBusinessPage = async (platform) => {
    if (!selectionTransactionId || !selectedOptionId) {
      setErrorMessage(`Select the ${PLATFORM_LABELS[platform] || platform} business page before continuing.`);
      setSuccessMessage("");
      return;
    }

    setSelectingPlatform(platform);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const response = await selectBusinessPage(platform, {
        transactionId: selectionTransactionId,
        selectionId: selectedOptionId,
      });
      const savedPage = Array.isArray(response?.businessPages)
        ? response.businessPages[0]
        : response?.businessPage || null;

      if (savedPage) {
        setBusinessPages((current) =>
          current.map((page) =>
            page.platform === platform
              ? normalizePlatformCapabilities(platform, savedPage, true)
              : page
          )
        );
      }

      clearSelectionFlow();
      setSuccessMessage(`${PLATFORM_LABELS[platform] || platform} business page connected successfully.`);
    } catch (error) {
      setErrorMessage(
        error?.response?.data?.message ||
          `Failed to connect ${PLATFORM_LABELS[platform] || platform} business page.`
      );
    } finally {
      setSelectingPlatform("");
    }
  };

  const handleManualFieldChange = (field) => (event) => {
    const value = event.target.value;
    setManualLinkedInForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleManualLinkedInSave = async () => {
    if (!manualLinkedInForm.accessToken.trim() || !manualLinkedInForm.externalUserId.trim()) {
      setErrorMessage("LinkedIn organization ID and access token are required for manual company setup.");
      setSuccessMessage("");
      return;
    }

    setSavingManualPlatform("LINKEDIN");
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const response = await saveBusinessPageManual("LINKEDIN", {
        enabled: true,
        accessToken: manualLinkedInForm.accessToken.trim(),
        externalUserId: manualLinkedInForm.externalUserId.trim(),
        displayName: manualLinkedInForm.displayName.trim() || undefined,
        pageUrl: manualLinkedInForm.pageUrl.trim() || undefined,
        username: manualLinkedInForm.username.trim() || undefined,
      });
      const savedPage = Array.isArray(response?.businessPages)
        ? response.businessPages[0]
        : null;
      if (savedPage) {
        setBusinessPages((current) =>
          current.map((page) =>
            page.platform === "LINKEDIN"
              ? normalizePlatformCapabilities("LINKEDIN", savedPage, true)
              : page
          )
        );
      }
      setManualLinkedInForm((current) => ({
        ...current,
        accessToken: "",
      }));
      setSuccessMessage("LinkedIn business page saved with manual company credentials.");
    } catch (error) {
      setErrorMessage(
        error?.response?.data?.message ||
          "Failed to save the LinkedIn business page with manual company credentials."
      );
    } finally {
      setSavingManualPlatform("");
    }
  };

  const renderLeaderStatuses = (leader) => {
    const statusByPlatform = new Map(
      (leader.platforms || []).map((platform) => [platform.platform, platform])
    );

    return PLATFORM_ORDER.map((platform) => {
      const status = statusByPlatform.get(platform);
      const connected = status?.status === "CONNECTED";
      const syncMeta = getLeaderSyncStatusMeta(status);
      return (
        <Card
          key={`${leader.userId}-${platform}`}
          variant="outlined"
          sx={{ p: 1.5, minWidth: 220, borderRadius: 2, borderColor: "#E2E8F0" }}
        >
          <Stack spacing={1}>
            <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
              <Typography sx={{ fontWeight: 600 }}>{PLATFORM_LABELS[platform]}</Typography>
              <Chip
                label={connected ? "Connected" : "Not Connected"}
                color={connected ? "success" : "default"}
                variant={connected ? "filled" : "outlined"}
                size="small"
              />
              <Chip
                label={syncMeta.label}
                color={syncMeta.color}
                variant={syncMeta.variant}
                size="small"
              />
            </Stack>
            <Typography sx={{ fontSize: 12, color: "#64748B" }}>
              Last success: {formatSyncDateTime(status?.lastSyncSuccessAt)}
            </Typography>
            <Typography sx={{ fontSize: 12, color: "#64748B" }}>
              Last attempt: {formatSyncDateTime(status?.lastSyncAttemptAt)}
            </Typography>
            {status?.lastSyncStatus === "FAILED" && status?.lastSyncError ? (
              <Typography sx={{ fontSize: 12, color: "#B42318", fontWeight: 500 }}>
                {status.lastSyncError}
              </Typography>
            ) : status?.lastSyncStatus === "SUCCESS_NO_NEW_CONTENT" ? (
              <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                Connection is healthy, but no new posts were imported in the current fetch window.
              </Typography>
            ) : (
              <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                {connected
                  ? "Leader content sync is tracked per platform."
                  : "Leader must connect this platform before sync can run."}
              </Typography>
            )}
          </Stack>
        </Card>
      );
    });
  };

  const activePage =
    businessPages.find((page) => page.platform === activePlatform) ||
    emptyPlatformState(activePlatform, false);
  const hasCapabilityData = typeof activePage.oauthSupported === "boolean";
  const supportsOauth = activePage.oauthSupported === true;
  const supportsManualFallback = activePage.manualFallbackAvailable === true;
  const oauthProviderLabel = activePage.oauthProviderLabel || "OAuth";
  const syncStatusMeta = getSyncStatusMeta(activePage);
  const busy =
    deletingPlatform === activePage.platform ||
    connectingPlatform === activePage.platform ||
    savingManualPlatform === activePage.platform;

  if (loading) {
    return (
      <Card
        elevation={0}
        sx={{ mt: 1, p: 4, borderRadius: 3, backgroundColor: "#FFFFFF" }}
      >
        <Stack alignItems="center" spacing={2}>
          <CircularProgress />
          <Typography sx={{ color: "#6B7280" }}>
            Loading organization settings...
          </Typography>
        </Stack>
      </Card>
    );
  }

  if (forbidden) {
    return (
      <Card
        elevation={0}
        sx={{ mt: 1, p: 4, borderRadius: 3, backgroundColor: "#FFFFFF" }}
      >
        <Alert severity="info">
          Only organization admins can view and manage business page settings.
          Leader connections remain personal and are managed by each leader from
          the regular social media integration page.
        </Alert>
      </Card>
    );
  }

  const platformSupportMessage = supportsOauth
    ? activePage.platform === "FACEBOOK" || activePage.platform === "INSTAGRAM"
      ? "Use the shared Meta app to connect the organization-owned page. The selected page token stays organization-owned and separate from employee personal integrations."
      : activePage.supportMessage
    : hasCapabilityData
      ? activePage.supportMessage ||
        "Save the organization-owned page metadata and access credentials used for business-page fetch and publishing."
      : "Connection options will appear after business page settings finish loading.";

  const capabilityAlertSeverity = supportsOauth
    ? "info"
    : hasCapabilityData
      ? "warning"
      : "info";

  const capabilityAlertMessage = supportsOauth
    ? `${PLATFORM_LABELS[activePage.platform]} business pages use the shared ${oauthProviderLabel} app. Leaders and employees still connect their own personal pages from the regular integration section.`
    : hasCapabilityData
      ? activePage.supportMessage ||
        "OAuth connection is not available for this platform yet. Use Advanced manual setup for now."
      : "Business page settings are still loading. Refresh this page if connection options do not appear.";

  return (
    <Stack spacing={2}>
      <Accordion
        expanded={businessExpanded}
        onChange={(_, expanded) => setBusinessExpanded(expanded)}
        sx={accordionSx}
      >
        <AccordionSummary
          expandIcon={<ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />}
          sx={accordionSummarySx}
        >
          <div className="reset-password">
            <div>
              <Typography variant="subtitle1" className="sub-title">
                Business Pages
              </Typography>
              <Typography variant="body2" className="mini-title">
                Manage organization-owned Facebook, Instagram, LinkedIn, and X
                pages in one place.
              </Typography>
            </div>
          </div>
        </AccordionSummary>

        <AccordionDetails sx={accordionDetailsSx}>
          {errorMessage && <Alert severity="error" sx={{ mb: 2 }}>{errorMessage}</Alert>}
          {successMessage && (
            <Alert severity="success" sx={{ mb: 2 }}>{successMessage}</Alert>
          )}

          <Box sx={{ borderBottom: "1px solid #E2E8F0", mb: 3 }}>
            <Tabs
              value={activePlatform}
              onChange={(_, value) => setActivePlatform(value)}
              variant="scrollable"
              scrollButtons="auto"
              sx={{
                "& .MuiTab-root": {
                  minHeight: 48,
                  fontWeight: 600,
                  textTransform: "none",
                },
                "& .Mui-selected": {
                  color: "#0047AB",
                },
                "& .MuiTabs-indicator": {
                  backgroundColor: "#0047AB",
                },
              }}
            >
              {PLATFORM_ORDER.map((platform) => (
                <Tab
                  key={platform}
                  value={platform}
                  label={PLATFORM_LABELS[platform]}
                />
              ))}
            </Tabs>
          </Box>

          <Card
            variant="outlined"
            sx={{ p: 3, borderRadius: 3, borderColor: "#E2E8F0" }}
          >
            <Stack spacing={2} sx={{ mb: 2 }}>
              <Box>
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {PLATFORM_LABELS[activePage.platform]}
                  </Typography>
                  <Chip
                    label={activePage.connected ? "Connected" : "Not Connected"}
                    color={activePage.connected ? "success" : "default"}
                    variant={activePage.connected ? "filled" : "outlined"}
                  />
                  <Chip
                    label={syncStatusMeta.label}
                    color={syncStatusMeta.color}
                    variant={syncStatusMeta.variant}
                  />
                </Stack>
                <Typography sx={{ color: "#6B7280", fontSize: 14 }}>
                  {platformSupportMessage}
                </Typography>
              </Box>
              <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
                <Chip
                  label={activePage.enabled ? "Fetch Enabled" : "Fetch Disabled"}
                  color={activePage.enabled ? "success" : "default"}
                  variant={activePage.enabled ? "filled" : "outlined"}
                  size="small"
                />
                {activePage.displayName ? (
                  <Chip
                    label={activePage.displayName}
                    variant="outlined"
                    size="small"
                  />
                ) : null}
              </Stack>
            </Stack>

            <Alert severity={capabilityAlertSeverity} sx={{ mb: 2 }}>
              {capabilityAlertMessage}
            </Alert>

            <Card
              variant="outlined"
              sx={{
                p: 2,
                mb: 2,
                borderRadius: 2,
                borderColor: "#E2E8F0",
                backgroundColor: "#F8FAFC",
              }}
            >
              <Stack spacing={1}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#0F172A" }}>
                  Sync Health
                </Typography>
                <Grid container spacing={1.5}>
                  <Grid item xs={12} md={4}>
                    <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                      Last successful sync
                    </Typography>
                    <Typography sx={{ fontWeight: 600 }}>
                      {formatSyncDateTime(activePage.lastSyncSuccessAt)}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                      Last attempt
                    </Typography>
                    <Typography sx={{ fontWeight: 600 }}>
                      {formatSyncDateTime(activePage.lastSyncAttemptAt)}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} md={4}>
                    <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                      Latest error time
                    </Typography>
                    <Typography sx={{ fontWeight: 600 }}>
                      {formatSyncDateTime(activePage.lastSyncErrorAt)}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                      Fetch cadence
                    </Typography>
                    <Typography sx={{ fontWeight: 600 }}>
                      {activePage.scheduleCadenceLabel || "Not scheduled"}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Typography sx={{ fontSize: 12, color: "#64748B" }}>
                      Next scheduled fetch
                    </Typography>
                    <Typography sx={{ fontWeight: 600 }}>
                      {formatSyncDateTime(activePage.nextScheduledFetchAt)}
                    </Typography>
                  </Grid>
                </Grid>
                {activePage.connected && typeof activePage.lastImportedCount === "number" ? (
                  <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                    {activePage.lastImportedCount > 0
                      ? `Last import inserted ${activePage.lastImportedCount} post${
                          activePage.lastImportedCount === 1 ? "" : "s"
                        }.`
                      : "No new posts were imported in the latest fetch window."}
                  </Typography>
                ) : null}
                {activePage.lastSyncStatus === "FAILED" && activePage.lastSyncError ? (
                  <Alert severity="error" sx={{ mt: 1 }}>
                    {activePage.lastSyncError}
                  </Alert>
                ) : (
                  <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                    {activePage.connected
                      ? "This platform triggers an immediate fetch after a successful connection and continues syncing on the scheduler."
                      : "Connect this platform to start an immediate fetch, scheduled syncs, and health tracking."}
                  </Typography>
                )}
              </Stack>
            </Card>

            {/* Manual LinkedIn setup — commented out, enable if client requests
            {activePage.platform === "LINKEDIN" && supportsManualFallback && !activePage.connected && (
              <Card variant="outlined" sx={{ p: 2, mb: 2, borderRadius: 2, borderColor: "#E2E8F0", backgroundColor: "#FFFDF7" }}>
                <Stack spacing={2}>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#0F172A" }}>Manual Company Fallback</Typography>
                    <Typography sx={{ fontSize: 13, color: "#64748B" }}>Use this only when LinkedIn OAuth discovery cannot return your company page.</Typography>
                  </Box>
                  <Grid container spacing={1.5}>
                    <Grid item xs={12} md={6}><TextField fullWidth label="LinkedIn Organization ID" value={manualLinkedInForm.externalUserId} onChange={handleManualFieldChange("externalUserId")} placeholder="12345678" /></Grid>
                    <Grid item xs={12} md={6}><TextField fullWidth label="Display Name" value={manualLinkedInForm.displayName} onChange={handleManualFieldChange("displayName")} placeholder="Moonhive Pvt Ltd" /></Grid>
                    <Grid item xs={12}><TextField fullWidth label="Access Token" value={manualLinkedInForm.accessToken} onChange={handleManualFieldChange("accessToken")} multiline minRows={3} /></Grid>
                    <Grid item xs={12} md={6}><TextField fullWidth label="Company Page URL" value={manualLinkedInForm.pageUrl} onChange={handleManualFieldChange("pageUrl")} placeholder="https://www.linkedin.com/company/your-company" /></Grid>
                    <Grid item xs={12} md={6}><TextField fullWidth label="Company Vanity Name" value={manualLinkedInForm.username} onChange={handleManualFieldChange("username")} placeholder="your-company" /></Grid>
                  </Grid>
                  <Stack direction="row" spacing={2}>
                    <Button variant="outlined" sx={{ borderColor: "#0047AB", color: "#0047AB" }} disabled={busy} onClick={handleManualLinkedInSave}>
                      {savingManualPlatform === "LINKEDIN" ? "Saving..." : activePage.connected ? "Save Manual Company Credentials" : "Use Manual Company Credentials"}
                    </Button>
                  </Stack>
                </Stack>
              </Card>
            )}
            */}

            {(activePage.platform === "X" || activePage.platform === "LINKEDIN") && selectionTransactionId && (
              <Card
                variant="outlined"
                sx={{
                  p: 2,
                  mb: 2,
                  borderRadius: 2,
                  borderColor: "#E2E8F0",
                  backgroundColor: "#FFFDF7",
                }}
              >
                <Stack spacing={2}>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#0F172A" }}>
                      {activePage.platform === "LINKEDIN" ? "Select LinkedIn Company Page" : "Confirm Connected X Handle"}
                    </Typography>
                    <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                      {activePage.platform === "LINKEDIN"
                        ? "Select the LinkedIn company page you want to connect as your organization business page."
                        : "Confirm that the connected handle below is the organization-owned brand account before saving it."}
                    </Typography>
                  </Box>

                  {selectionLoading ? (
                    <Stack direction="row" spacing={1} alignItems="center">
                      <CircularProgress size={18} />
                      <Typography sx={{ color: "#64748B", fontSize: 14 }}>
                        {activePage.platform === "LINKEDIN" ? "Loading LinkedIn company pages..." : "Loading connected X account..."}
                      </Typography>
                    </Stack>
                  ) : selectionOptions.length === 0 && selectionTransactionId ? (
                    <Stack spacing={1.5}>
                      <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                        Pages not loaded yet. Click below to fetch available pages.
                      </Typography>
                      <Button
                        variant="outlined"
                        sx={{ borderColor: "#94A3B8", color: "#64748B", textTransform: "none" }}
                        disabled={selectionLoading}
                        onClick={async () => {
                          setSelectionLoading(true);
                          try {
                            const response = await getAvailableBusinessPages(activePage.platform, selectionTransactionId);
                            const options = normalizeSelectionOptions(response);
                            setSelectionOptions(options);
                            setSelectedOptionId(options[0]?.selectionId || "");
                            if (!options.length) {
                              setErrorMessage("No business pages found for this transaction.");
                            }
                          } catch (error) {
                            setErrorMessage(error?.response?.data?.message || "Failed to load available pages.");
                          } finally {
                            setSelectionLoading(false);
                          }
                        }}
                      >
                        Load Available Pages
                      </Button>
                    </Stack>
                  ) : selectionOptions.length > 0 ? (
                    <Stack spacing={1.5}>
                      <TextField
                        select
                        fullWidth
                        label={activePage.platform === "LINKEDIN" ? "Select Company Page" : "Connected X Handle"}
                        value={selectedOptionId}
                        onChange={(event) => setSelectedOptionId(event.target.value)}
                        SelectProps={{ native: true }}
                      >
                        {selectionOptions.map((option) => (
                          <option key={option.selectionId} value={option.selectionId}>
                            {option.displayName}
                            {option.username ? ` (@${option.username})` : ""}
                          </option>
                        ))}
                      </TextField>

                      {selectionOptions
                        .filter((option) => option.selectionId === selectedOptionId)
                        .map((option) => (
                          <Stack key={option.selectionId} spacing={0.5}>
                            {option.username ? (
                              <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                                Handle: @{option.username}
                              </Typography>
                            ) : null}
                            {option.externalUserId ? (
                              <Typography sx={{ fontSize: 13, color: "#64748B" }}>
                                Account ID: {option.externalUserId}
                              </Typography>
                            ) : null}
                            {option.pageUrl ? (
                              <Typography sx={{ fontSize: 13, color: "#64748B", wordBreak: "break-all" }}>
                                Profile URL: {option.pageUrl}
                              </Typography>
                            ) : null}
                          </Stack>
                        ))}
                    </Stack>
                  ) : (
                    <Alert severity="warning">
                      No business pages are available to select for this transaction.
                    </Alert>
                  )}

                  <Stack direction="row" spacing={2}>
                    <Button
                      variant="contained"
                      sx={{ backgroundColor: "#0047AB" }}
                      disabled={
                        selectionLoading ||
                        selectingPlatform === activePage.platform ||
                        !selectedOptionId
                      }
                      onClick={() => handleSelectBusinessPage(activePage.platform)}
                    >
                      {selectingPlatform === activePage.platform
                        ? "Saving..."
                        : activePage.platform === "LINKEDIN"
                          ? "Connect LinkedIn Company Page"
                          : "Confirm and Connect X Account"}
                    </Button>
                    <Button
                      variant="outlined"
                      sx={{ borderColor: "#94A3B8", color: "#64748B" }}
                      disabled={selectionLoading || selectingPlatform === activePage.platform}
                      onClick={clearSelectionFlow}
                    >
                      Cancel
                    </Button>
                  </Stack>
                </Stack>
              </Card>
            )}

            <Card
              variant="outlined"
              sx={{
                p: 2,
                mt: 2,
                borderRadius: 2,
                borderColor: "#E2E8F0",
                backgroundColor: "#FFFFFF",
              }}
            >
              <Grid container spacing={1.5}>
                <Grid item xs={12} md={6}>
                  <Typography sx={{ fontSize: 12, color: "#64748B" }}>Connected page</Typography>
                  <Typography sx={{ fontWeight: 600 }}>
                    {activePage.displayName || activePage.username || "Not connected"}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Typography sx={{ fontSize: 12, color: "#64748B" }}>Page identifier</Typography>
                  <Typography sx={{ fontWeight: 600 }}>
                    {activePage.pageId || activePage.externalUserId || "Not available"}
                  </Typography>
                </Grid>
                <Grid item xs={12}>
                  <Typography sx={{ fontSize: 12, color: "#64748B" }}>Business page link</Typography>
                  <Typography sx={{ fontWeight: 600, wordBreak: "break-all" }}>
                    {activePage.businessPageLink || activePage.pageUrl || "Not available"}
                  </Typography>
                </Grid>
              </Grid>
            </Card>

            <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
              {supportsOauth ? (
                <Button
                  variant="contained"
                  sx={{ backgroundColor: "#0047AB" }}
                  disabled={busy}
                  onClick={() => handleConnect(activePage.platform)}
                >
                  {connectingPlatform === activePage.platform
                    ? "Redirecting..."
                    : activePage.connected
                      ? `Reconnect with ${oauthProviderLabel}`
                      : `Connect with ${oauthProviderLabel}`}
                </Button>
              ) : hasCapabilityData ? (
                <Button
                  variant="outlined"
                  sx={{ borderColor: "#94A3B8", color: "#64748B" }}
                  disabled
                >
                  {activePage.platform === "LINKEDIN" && supportsManualFallback
                    ? "Manual Setup Only"
                    : "Connection Unavailable"}
                </Button>
              ) : (
                <Button
                  variant="outlined"
                  sx={{ borderColor: "#94A3B8", color: "#64748B" }}
                  disabled
                >
                  Loading Connection Options
                </Button>
              )}
              <Button
                variant="outlined"
                color="error"
                disabled={busy || !activePage.connected}
                onClick={() => handleDisconnect(activePage.platform)}
              >
                {deletingPlatform === activePage.platform
                  ? "Disconnecting..."
                  : "Disconnect"}
              </Button>
            </Stack>
          </Card>
        </AccordionDetails>
      </Accordion>

      <Accordion
        expanded={leadersExpanded}
        onChange={(_, expanded) => setLeadersExpanded(expanded)}
        sx={accordionSx}
      >
        <AccordionSummary
          expandIcon={<ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />}
          sx={accordionSummarySx}
        >
          <div className="reset-password">
            <div>
              <Typography variant="subtitle1" className="sub-title">
                Leader Page Connection Status
              </Typography>
              <Typography variant="body2" className="mini-title">
                Review leader-owned social connections without editing personal
                accounts.
              </Typography>
            </div>
          </div>
        </AccordionSummary>

        <AccordionDetails sx={accordionDetailsSx}>
          <Stack spacing={1.5} sx={{ mb: 3 }}>
            <Typography sx={{ color: "#6B7280" }}>
              Leaders appear here automatically based on the user role flags.
              This section is read-only: leaders manage their own personal
              social connections from the regular integration flow.
            </Typography>
          </Stack>

          {leaderErrorMessage ? (
            <Alert severity="warning">{leaderErrorMessage}</Alert>
          ) : leaders.length === 0 ? (
            <Alert severity="info">
              No leaders are currently marked for this organization.
            </Alert>
          ) : (
            <Stack spacing={2.5}>
              {leaders.map((leader, index) => (
                <Box key={leader.userId}>
                  <Stack
                    direction={{ xs: "column", md: "row" }}
                    justifyContent="space-between"
                    spacing={2}
                    alignItems={{ xs: "flex-start", md: "center" }}
                  >
                    <Box>
                      <Typography sx={{ fontWeight: 600 }}>
                        {leader.name || leader.email}
                      </Typography>
                      <Typography sx={{ color: "#6B7280", fontSize: 14 }}>
                        {leader.email}
                      </Typography>
                    </Box>
                    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                      {renderLeaderStatuses(leader)}
                    </Stack>
                  </Stack>
                  {index < leaders.length - 1 && <Divider sx={{ mt: 2.5 }} />}
                </Box>
              ))}
            </Stack>
          )}
        </AccordionDetails>
      </Accordion>
    </Stack>
  );
};

export default OrganizationSettings;
