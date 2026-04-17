import React, { useEffect, useState } from "react";
import "./Setting.css";
import {
  Box,
  Button,
  Card,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemIcon,
  ListItemSecondaryAction,
  ListItemText,
  Stack,
  Switch,
  TextField,
  Typography,
  Accordion,
  AccordionDetails,
  AccordionSummary,
} from "@mui/material";
import { Close } from "@mui/icons-material";
import insta from "../../../assets/socialmedia/instagram.svg";
import facebook from "../../../assets/socialmedia/faceBuk.svg";
import linkedin from "../../../assets/socialmedia/linkdn.svg";
import x from "../../../assets/socialmedia/x.svg";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import CloseIcon from "@mui/icons-material/Close";
import SchedulePostModal from "./SetSchdule";
import ResetPasswordModal from "./ResetPasswordModal";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import {
  getBusinessPageLinks,
  deleteScheduleExternalPost,
  enablePlatforms,
  getAllPlatforms,
  getScheduleExternalPost,
  updateScheduleExternalPost,
} from "../../../services/postService";
import {
  fetchOrgTrendingTopics,
  saveOrgTrendingTopics,
  employeeTrendingTopics,
} from "../../../services/adminServices";
import EditScheduleModal from "./EditScheduleModal";
import RescheduleModal from "./RescheduleModal";
import EditIcon from "@mui/icons-material/Edit";
import ScheduleIcon from "@mui/icons-material/Schedule";
import { toast } from "react-toastify";
import { Auth } from "../../../contexts/AuthContext";
import OrganizationSettings from "./OrganizationSettings";
import { useLocation, useNavigate } from "react-router-dom";

const socialMediaPlatforms = [
  {
    name: "Instagram",
    icon: <img src={insta} alt="insta" />,
    key: "instagram",
  },
  {
    name: "Facebook",
    icon: <img src={facebook} alt="facebook" />,
    key: "facebook",
  },
  { name: "Twitter / X", icon: <img src={x} alt="twitter" />, key: "twitter" },
  {
    name: "LinkedIn",
    icon: <img src={linkedin} alt="linkedin" />,
    key: "linkedin",
  },
];

const parseTopics = (value) => {
  return value
    .split(",")
    .map((topic) => topic.trim())
    .filter(Boolean);
};

const buildBusinessPagesFromTrackSelf = (socialMediaConnectionData = []) =>
  socialMediaConnectionData.reduce((acc, item) => {
    const platformKey = item?.platform?.toLowerCase();
    if (!platformKey) {
      return acc;
    }
    const normalizedKey =
      platformKey === "x" || platformKey === "twitter" ? "x" : platformKey;
    acc[normalizedKey] = {
      url: item?.businessPageLink || "",
      username: "",
      tagText: "",
      oauthSourcePage: item?.oauthSourcePage || null,
    };
    return acc;
  }, {});

const extractBusinessPageLinks = (businessPages = {}) =>
  Object.entries(businessPages).reduce((acc, [platform, info]) => {
    acc[platform] = info?.url || "";
    return acc;
  }, {});

const getStoredRoles = () => {
  const rawRole = sessionStorage.getItem("role");
  if (!rawRole) {
    return [];
  }

  try {
    const parsedRole = JSON.parse(rawRole);
    return Array.isArray(parsedRole) ? parsedRole : [parsedRole];
  } catch (error) {
    return [rawRole];
  }
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

const Settings = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [expandIntegrations, setExpandIntegrations] = useState(false);
  const [socialMedia, setSocialMedia] = useState({
    instagram: false,
    facebook: false,
    twitter: false,
    linkedin: false,
  });
  const [platformToggler, setPlatformToggler] = useState(false);
  const [isDisconnect, setIsDisconnect] = useState(false);
  const [selectedPlatform, setSelectedPlatform] = useState(null);
  const [isPlatformActionLoading, setIsPlatformActionLoading] = useState(false);
  const [platformActionError, setPlatformActionError] = useState("");
  const [discard, setDiscard] = useState(false);
  const [deleteScheduleLoading, setDeleteScheduleLoading] = useState(false);
  const [openResetPassword, setOpenResetPassword] = useState(false);
  const [open, setOpen] = useState(false);
  const [schedulePosts, setSchedulePosts] = useState([]);
  const [selectedId, setSelectedId] = useState();
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [rescheduleModalOpen, setRescheduleModalOpen] = useState(false);
  const [selectedSchedulePost, setSelectedSchedulePost] = useState(null);
  const [isAdminUser, setIsAdminUser] = useState(false);
  const [settingsLoading, setSettingsLoading] = useState(true);
  const [, setSettingsError] = useState(null);
  const [trendingTopics, setTrendingTopics] = useState([]);
  const [trendingInput, setTrendingInput] = useState("");
  const [isSavingTrending, setIsSavingTrending] = useState(false);
  const [trendingHelperText, setTrendingHelperText] = useState("");
  const [limitExceeded, setLimitExceeded] = useState(false);
  const TRENDING_LIMIT = 30;
  const [trendingExpanded, setTrendingExpanded] = useState(false);
  const [scheduleExpanded, setScheduleExpanded] = useState(false);
  const [resetPasswordExpanded, setResetPasswordExpanded] = useState(false);
  const [isLoadingSchedule, setIsLoadingSchedule] = useState(false);
  const [scheduleErrorMessage, setScheduleErrorMessage] = useState("");
  const { setBusinessPages } = Auth();

  const resolveSourcePage = () =>
    isAdminUser ? "ADMIN_SETTINGS" : "EMPLOYEE_SETTINGS";

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const status = params.get("socialConnect");
    const platform = params.get("platform");
    const reason = params.get("reason");

    if (!status || !platform) {
      return;
    }

    const platformLabel =
      socialMediaPlatforms.find(
        (item) => item.key === platform.toLowerCase() || (platform.toLowerCase() === "x" && item.key === "twitter")
      )?.name || platform;

    if (status === "success") {
      toast.success(`${platformLabel} connected successfully.`);
      fetchAllSocialMedia();
    } else if (status === "error") {
      const reasonLabel = reason
        ? reason.replace(/_/g, " ")
        : "Please try again.";
      toast.error(`${platformLabel} connection failed: ${reasonLabel}.`);
    }

    params.delete("socialConnect");
    params.delete("platform");
    params.delete("reason");
    navigate(
      {
        pathname: location.pathname,
        search: params.toString() ? `?${params.toString()}` : "",
      },
      { replace: true }
    );
  }, [location.pathname, location.search, navigate]);

  const handleConfirm = async () => {
    setDeleteScheduleLoading(true);
    try {
      const response = await deleteScheduleExternalPost(selectedId);
      if (response?.status === true) {
        fetchAllSchedule();
        toast.success("scheduled post successfully deleted");
      }
    } catch (error) {
      console.log(error);
    } finally {
      setDeleteScheduleLoading(false);
      setDiscard(false);
    }
  };
  const handleDiscard = (id) => {
    setSelectedId(id);
    setDiscard(true);
  };

  const handleToggle = (platform) => {
    const isAlreadyConnected = socialMedia[platform];

    // ✅ if platform is "twitter", set to "X"
    const selected = platform.toLowerCase() === "twitter" ? "X" : platform;

    setSelectedPlatform(selected);
    setIsDisconnect(isAlreadyConnected);
    setPlatformActionError("");
    setPlatformToggler(true);
  };

  const handleSchedule = () => {
    setOpen(true);
  };

  const fetchAllSocialMedia = async () => {
    setSettingsLoading(true);
    setSettingsError(null);
    try {
      const response = await getAllPlatforms();
      if (response?.status === true) {
        const data = response?.socialMediaConnectionData || [];

        // map API data to state
        const updatedSocialMedia = {
          instagram: false,
          facebook: false,
          twitter: false,
          linkedin: false,
        };

        data.forEach((item) => {
          if (item.status === "CONNECTED") {
            if (
              item.platform.toLowerCase() === "x" ||
              item.platform.toLowerCase() === "twitter"
            ) {
              updatedSocialMedia.twitter = true;
            } else if (item.platform.toLowerCase() === "instagram") {
              updatedSocialMedia.instagram = true;
            } else if (item.platform.toLowerCase() === "facebook") {
              updatedSocialMedia.facebook = true;
            } else if (item.platform.toLowerCase() === "linkedin") {
              updatedSocialMedia.linkedin = true;
            }
          }
        });

        // set state
        setSocialMedia(updatedSocialMedia); // ✅ Auto toggle ON for connected platforms
        const businessPages = buildBusinessPagesFromTrackSelf(data);
        setBusinessPages(businessPages);
        sessionStorage.setItem("businessPages", JSON.stringify(businessPages));
        sessionStorage.setItem(
          "businessPageLinks",
          JSON.stringify(extractBusinessPageLinks(businessPages))
        );
      }
    } catch (error) {
      console.log(error);
      setSettingsError("Failed to load settings. Please refresh the page.");
    } finally {
      setSettingsLoading(false);
    }
  };

  const enablePlatform = async () => {
    if (!selectedPlatform) {
      return;
    }

    setIsPlatformActionLoading(true);
    setPlatformActionError("");

    try {
      const payload = {
        platform: selectedPlatform.toUpperCase(),
        connect: !isDisconnect, // false if disconnecting
        sourcePage: resolveSourcePage(),
      };

      const response = await enablePlatforms(payload);

      if (response?.authUrl) {
        window.location.href = response?.authUrl;
        return;
      }

      if (response?.status === true) {
        setSocialMedia((prev) => ({
          ...prev,
          [selectedPlatform.toLowerCase()]: !isDisconnect,
        }));
        fetchAllSocialMedia();
        const linksResponse = await getBusinessPageLinks();
        if (linksResponse?.status === true) {
          const businessPages = linksResponse?.businessPages || {};
          setBusinessPages(businessPages);
          sessionStorage.setItem("businessPages", JSON.stringify(businessPages));
          sessionStorage.setItem(
            "businessPageLinks",
            JSON.stringify(extractBusinessPageLinks(businessPages))
          );
        }
        setPlatformToggler(false);
        return;
      }

      const fallbackMessage =
        response?.message ||
        `Unable to ${isDisconnect ? "disconnect" : "connect"} ${selectedPlatform}.`;
      setPlatformActionError(fallbackMessage);
      toast.error(fallbackMessage);
    } catch (error) {
      const errorMessage =
        error?.message ||
        `Unable to ${isDisconnect ? "disconnect" : "connect"} ${selectedPlatform}.`;
      setPlatformActionError(errorMessage);
      toast.error(errorMessage);
    } finally {
      setIsPlatformActionLoading(false);
    }
  };

  useEffect(() => {
    fetchAllSocialMedia();
  }, []);

  useEffect(() => {
    const admin = getStoredRoles().includes("ROLE_ADMIN");
    setIsAdminUser(admin);
    if (admin) {
      loadTrendingTopics();
    } else {
      loadEmployeeTrendingTopics();
    }
  }, []);

  const loadEmployeeTrendingTopics = async () => {
    try {
      const response = await employeeTrendingTopics();
      const topics = response?.trendingTopics || [];
      setTrendingTopics(topics);
    } catch (error) {
      console.error("Unable to load trending topics", error);
    }
  };

  const loadTrendingTopics = async () => {
    try {
      const response = await fetchOrgTrendingTopics();
      const topics = response?.trendingTopics || response?.topics || [];
      const limited = topics.slice(0, TRENDING_LIMIT);
      setTrendingTopics(limited);
      setTrendingInput(limited.join(", "));
      if (topics.length > TRENDING_LIMIT) {
        setLimitExceeded(true);
        setTrendingHelperText(`Maximum ${TRENDING_LIMIT} topics allowed.`);
      } else {
        setLimitExceeded(false);
        setTrendingHelperText("");
      }
    } catch (error) {
      console.error("Unable to load trending topics", error);
    }
  };

  const handleTrendingInputChange = (event) => {
    const value = event?.target?.value || "";
    const parsed = parseTopics(value);
    const uniqueTopics = Array.from(new Set(parsed));
    const limited = uniqueTopics.slice(0, TRENDING_LIMIT);
    setTrendingInput(value);
    setTrendingTopics(limited);
    if (uniqueTopics.length > TRENDING_LIMIT) {
      setLimitExceeded(true);
      setTrendingHelperText(`Maximum ${TRENDING_LIMIT} topics allowed.`);
    } else {
      setLimitExceeded(false);
      setTrendingHelperText("");
    }
  };

  const handleRemoveTopic = (topicToRemove) => {
    const updatedTopics = trendingTopics.filter(
      (topic) => topic !== topicToRemove
    );
    setTrendingTopics(updatedTopics);
    setTrendingInput(updatedTopics.join(", "));
    setLimitExceeded(false);
    setTrendingHelperText("");
  };

  const handleTrendingSave = async () => {
    if (limitExceeded) {
      setTrendingHelperText(`Maximum ${TRENDING_LIMIT} topics allowed.`);
      return;
    }
    if (trendingTopics.length === 0) {
      setTrendingHelperText("Add at least one topic.");
      return;
    }

    setIsSavingTrending(true);
    try {
      await saveOrgTrendingTopics(trendingTopics);
      setTrendingHelperText("");
      toast.success("Trending topics updated.");
    } catch (error) {
      setTrendingHelperText("Unable to save topics right now.");
      toast.error(error.message || "Failed to save trending topics.");
    } finally {
      setIsSavingTrending(false);
    }
  };

  const handleTrendingClear = async () => {
    if (isSavingTrending) {
      return;
    }
    setIsSavingTrending(true);
    try {
      await saveOrgTrendingTopics([]);
      setTrendingTopics([]);
      setTrendingInput("");
      setLimitExceeded(false);
      setTrendingHelperText("Custom preferences cleared. Showing default topics.");
      toast.success("Trending topic preferences reset.");
    } catch (error) {
      setTrendingHelperText("Unable to clear topics right now.");
      toast.error(error.message || "Failed to clear trending topics.");
    } finally {
      setIsSavingTrending(false);
    }
  };

  const fetchAllSchedule = async () => {
    setIsLoadingSchedule(true);
    setScheduleErrorMessage("");
    try {
      const response = await getScheduleExternalPost();
      if (response?.status === true) {
        setSchedulePosts(response?.schedulePostList);
      } else {
        setSchedulePosts([]);
      }
    } catch (error) {
      setSchedulePosts([]);
      setScheduleErrorMessage(
        error.message ||
          "Scheduler service is temporarily unavailable. Please retry in a moment."
      );
      toast.error(
        error.message ||
          "Scheduler service is temporarily unavailable. Please retry in a moment."
      );
    } finally {
      setIsLoadingSchedule(false);
    }
  };

  const openAccordian = () => {
    fetchAllSchedule();
  };

  const handleCancel = () => {
    setSelectedId();
    setDiscard(false);
  };

  const handleEditView = (post) => {
    setSelectedSchedulePost(post);
    setEditModalOpen(true);
  };

  const handleReschedule = (post) => {
    setSelectedSchedulePost(post);
    setRescheduleModalOpen(true);
  };

  const handleEditSave = async (updatedData) => {
    try {
      // Map platforms to uppercase format for API
      const platformMapping = {
        instagram: "INSTAGRAM",
        facebook: "FACEBOOK",
        linkedin: "LINKEDIN",
        twitter: "X",
        "twitter/x": "X",
        x: "X",
      };
      const formattedPlatforms = (updatedData.platforms || []).map(
        (p) => platformMapping[p.toLowerCase()] || p.toUpperCase()
      );

      const payload = {
        id: updatedData.id,
        postId: updatedData.postId,
        content: updatedData.content,
        type: updatedData.type,
        platforms: formattedPlatforms,
        scheduledTimeUtc: updatedData.scheduledTimeUtc,
      };
      const response = await updateScheduleExternalPost(payload);
      if (response?.status === true) {
        toast.success("Post updated successfully");
        fetchAllSchedule();
        setEditModalOpen(false);
        setSelectedSchedulePost(null);
      }
    } catch (error) {
      console.log(error);
      toast.error("Failed to update post");
    }
  };

  const handleRescheduleSave = async (updatedData) => {
    try {
      // Map platforms to uppercase format for API
      const platformMapping = {
        instagram: "INSTAGRAM",
        facebook: "FACEBOOK",
        linkedin: "LINKEDIN",
        twitter: "X",
        "twitter/x": "X",
        x: "X",
      };
      const formattedPlatforms = (updatedData.platforms || []).map(
        (p) => platformMapping[p.toLowerCase()] || p.toUpperCase()
      );

      const payload = {
        id: updatedData.id,
        postId: updatedData.postId,
        content: updatedData.content,
        type: "POST",
        platforms: formattedPlatforms,
        scheduledTimeUtc: updatedData.scheduledTimeUtc,
      };
      const response = await updateScheduleExternalPost(payload);
      if (response?.status === true) {
        toast.success("Post rescheduled successfully");
        fetchAllSchedule();
        setRescheduleModalOpen(false);
        setSelectedSchedulePost(null);
      }
    } catch (error) {
      console.log(error);
      toast.error("Failed to reschedule post");
    }
  };

  return (
    <Card className="settings-container">
      <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        {isAdminUser && <OrganizationSettings />}

        <Accordion
          expanded={trendingExpanded}
          onChange={(_, isExpanded) => setTrendingExpanded(isExpanded)}
          sx={accordionSx}
          className="trending-accordion"
        >
          <AccordionSummary
            expandIcon={<ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />}
            sx={accordionSummarySx}
          >
            <div className="reset-password trending-summary">
              <div>
                <Typography variant="subtitle1" className="sub-title">
                  Trending Topic Preferences
                </Typography>
                <Typography variant="body2" className="mini-title">
                  {isAdminUser
                    ? `Add up to ${TRENDING_LIMIT} comma-separated topics to highlight what is trending for your organization.`
                    : "Topics configured by your organization admin."}
                </Typography>
              </div>
            </div>
          </AccordionSummary>
          <AccordionDetails sx={accordionDetailsSx}>
            {isAdminUser && (
              <TextField
                placeholder="e.g. Sustainability, Policy, AI Adoption"
                multiline
                minRows={3}
                value={trendingInput}
                onChange={handleTrendingInputChange}
                className="trending-textarea"
                fullWidth
              />
            )}
            <Box className="trending-chip-row">
              {trendingTopics.length > 0 ? (
                trendingTopics.map((topic) => (
                  <Chip
                    key={topic}
                    label={topic}
                    size="small"
                    onDelete={isAdminUser ? () => handleRemoveTopic(topic) : undefined}
                    sx={{
                      backgroundColor: "#EDEFF2",
                      borderRadius: "12px",
                      fontSize: "12px",
                    }}
                  />
                ))
              ) : (
                !isAdminUser && (
                  <Typography variant="body2" color="#95919D">
                    No trending topics configured yet.
                  </Typography>
                )
              )}
            </Box>
            {isAdminUser && (
              <>
                <Typography
                  variant="body2"
                  className={`helper-text ${limitExceeded ? "error" : ""}`}
                >
                  {trendingHelperText || `Separate topics with commas; max ${TRENDING_LIMIT}.`}
                </Typography>
                <Box className="trending-actions">
                  <Button
                    variant="outlined"
                    color="primary"
                    onClick={handleTrendingClear}
                    disabled={isSavingTrending || trendingTopics.length === 0}
                  >
                    Clear Preferences
                  </Button>
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={handleTrendingSave}
                    disabled={
                      isSavingTrending || trendingTopics.length === 0 || limitExceeded
                    }
                  >
                    {isSavingTrending ? "Saving..." : "Save Preferences"}
                  </Button>
                </Box>
              </>
            )}
          </AccordionDetails>
        </Accordion>

        <Accordion
          expanded={expandIntegrations}
          onChange={(_, isExpanded) => setExpandIntegrations(isExpanded)}
          sx={accordionSx}
        >
          <AccordionSummary
            expandIcon={
              <ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />
            }
            sx={accordionSummarySx}
          >
            <div className="reset-password">
              <div>
                <Typography variant="subtitle1" className="sub-title">
                  Personal Accounts
                </Typography>
                <Typography variant="body2" className="mini-title">
                  Connect your own social profiles for sharing content from the
                  app. These connections are personal and separate from
                  organization business pages.
                </Typography>
              </div>
            </div>
          </AccordionSummary>

          <AccordionDetails sx={accordionDetailsSx}>
            <List>
              {socialMediaPlatforms?.map(({ name, icon, key }) => (
                <ListItem key={key} className="social-media-item">
                  <ListItemIcon>{icon}</ListItemIcon>
                  <ListItemText primary={name} />
                  <ListItemSecondaryAction>
                    <Switch
                      checked={socialMedia[key]}
                      onChange={() => handleToggle(key)}
                      disabled={isPlatformActionLoading}
                    />
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          </AccordionDetails>
        </Accordion>

        <Accordion
          expanded={scheduleExpanded}
          onChange={(_, isExpanded) => {
            setScheduleExpanded(isExpanded);
            if (isExpanded) {
              openAccordian();
            }
          }}
          sx={accordionSx}
        >
          <AccordionSummary
            expandIcon={
              <ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />
            }
            sx={accordionSummarySx}
          >
            <div className="reset-password">
              <div>
                <Typography variant="subtitle1" className="sub-title">
                  Schedule
                </Typography>
                <Typography variant="body2" className="mini-title">
                  Review and manage queued posts that will publish to connected
                  socials at a scheduled time.
                </Typography>
              </div>
            </div>
          </AccordionSummary>

          <AccordionDetails
            sx={{
              ...accordionDetailsSx,
              maxHeight: "240px",
              overflowY: "auto",
              "&::-webkit-scrollbar": { width: "6px" },
              "&::-webkit-scrollbar-thumb": {
                backgroundColor: "#ccc",
                borderRadius: "4px",
              },
            }}
          >
            <Box
              sx={{
                display: "flex",
                justifyContent: "flex-end",
                mb: 2,
              }}
            >
              <Button
                variant="contained"
                sx={{ backgroundColor: "#0047AB" }}
                onClick={handleSchedule}
              >
                New Schedule
              </Button>
            </Box>
            {scheduleErrorMessage ? (
              <Typography
                variant="body2"
                sx={{ color: "#CC2D1A", mb: 2, fontWeight: 500 }}
              >
                {scheduleErrorMessage}
              </Typography>
            ) : null}
            {isLoadingSchedule ? (
              <Typography variant="body2" sx={{ color: "#6B7280", mb: 2 }}>
                Loading scheduled posts...
              </Typography>
            ) : null}
            <ul
              style={{
                margin: 0,
                paddingLeft: "0px",
                listStyleType: "none",
              }}
            >
              {schedulePosts?.length > 0 ? (
                schedulePosts?.map((item, i) => {
                  const localTime = new Date(
                    item.scheduledTimeUtc
                  ).toLocaleString("en-IN", {
                    timeZone: "Asia/Kolkata",
                    dateStyle: "medium",
                    timeStyle: "short",
                  });
                  const getStatusConfig = () => {
                    if (item.isProcessed !== true) {
                      return {
                        label: "Not Processed",
                        bg: "#F5F5F5",
                        color: "#757575",
                        border: "#BDBDBD",
                      };
                    }
                    if (item.isSuccess === true) {
                      return {
                        label: "Success",
                        bg: "#DBFFDF",
                        color: "#21AD0F",
                        border: "#21AD0F",
                      };
                    }
                    return {
                      label: "Failed",
                      bg: "#FED6D6",
                      color: "#CC2D1A",
                      border: "#CC2D1A",
                    };
                  };

                  const statusConfig = getStatusConfig();
                  return (
                    <li key={item.id} style={{ padding: "6px 0px" }}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", flexWrap: "wrap" }}>
                          <span style={{ fontWeight: 500, fontSize: "14px" }}>
                            {i + 1}.
                          </span>{" "}
                          <span style={{ color: "#95919D" }}>
                            {item?.content ? item.content : "No content"}{" "}
                            scheduled for{"    "}
                          </span>
                          <span style={{ fontWeight: 500, fontSize: "15px" }}>
                            {localTime}
                          </span>
                          <Chip
                            label={statusConfig.label}
                            size="small"
                            sx={{
                              backgroundColor: statusConfig.bg,
                              color: statusConfig.color,
                              fontSize: "11px",
                              fontWeight: 400,
                              borderRadius: "12px",
                              border: `1px solid ${statusConfig.border}`,
                              height: "22px",
                              ml: 1,
                            }}
                          />
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <IconButton
                            onClick={() => handleEditView(item)}
                            title={item.isProcessed ? "Already published" : "Edit"}
                            disabled={item.isProcessed}
                          >
                            <EditIcon
                              sx={{
                                color: item.isProcessed ? "#CBD5E1" : "#2D76DC",
                                width: "14px",
                                height: "14px",
                              }}
                            />
                          </IconButton>
                          <IconButton
                            onClick={() => handleReschedule(item)}
                            title="Reschedule"
                          >
                            <ScheduleIcon
                              sx={{
                                color: "#0047AB",
                                width: "14px",
                                height: "14px",
                              }}
                            />
                          </IconButton>
                          <IconButton
                            onClick={() => handleDiscard(item?.id)}
                            title="Delete"
                          >
                            <DeleteOutlineIcon
                              sx={{
                                color: "#DA4040",
                                width: "14px",
                                height: "14px",
                              }}
                            />
                          </IconButton>
                        </div>
                      </div>
                    </li>
                  );
                })
              ) : (
                <li>No scheduled posts</li>
              )}
            </ul>
          </AccordionDetails>
        </Accordion>

        <Accordion
          expanded={resetPasswordExpanded}
          onChange={(_, isExpanded) => setResetPasswordExpanded(isExpanded)}
          sx={accordionSx}
        >
          <AccordionSummary
            expandIcon={
              <ExpandMoreIcon sx={{ color: "#2D76DC", fontSize: "18px" }} />
            }
            sx={accordionSummarySx}
          >
            <div className="reset-password">
              <div>
                <Typography variant="subtitle1" className="sub-title">
                  Reset Password
                </Typography>
                <Typography variant="body2" className="mini-title">
                  Secure your account by resetting your password.
                </Typography>
              </div>
            </div>
          </AccordionSummary>

          <AccordionDetails sx={accordionDetailsSx}>
            <Stack
              direction={{ xs: "column", sm: "row" }}
              justifyContent="space-between"
              alignItems={{ xs: "flex-start", sm: "center" }}
              spacing={2}
            >
              <Typography sx={{ color: "#6B7280" }}>
                Update your password here without leaving the settings page.
              </Typography>
              <Button
                variant="contained"
                sx={{ backgroundColor: "#0047AB" }}
                onClick={() => setOpenResetPassword(true)}
              >
                Reset Password
              </Button>
            </Stack>
          </AccordionDetails>
        </Accordion>
      </Box>

      <Dialog
        open={discard}
        onClose={() => setDiscard(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: "20px",
            padding: "24px",
            position: "relative",
          },
        }}
      >
        {/* Close Icon */}
        <IconButton
          onClick={() => setDiscard(false)}
          sx={{ position: "absolute", top: 12, right: 12 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>

        {/* Title */}
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "24px",
            p: 0,
            mb: 1,
          }}
        >
          Delete schedule?
        </DialogTitle>

        {/* Subtitle */}
        <DialogContent sx={{ px: 0 }}>
          <Typography
            sx={{
              color: "#95919D",
              fontSize: "16px",
              fontWeight: 400,
              width: "90%",
            }}
          >
            Deleting this schedule will stop the automatic sharing to your
            platforms
          </Typography>
        </DialogContent>

        {/* Buttons */}
        <DialogActions
          sx={{
            px: 0,
            pt: 4,
            display: "flex",
            justifyContent: "end",
            gap: 2,
          }}
        >
          <Button
            onClick={handleCancel}
            disabled={deleteScheduleLoading}
            sx={{
              backgroundColor: "#F4F0FF",
              color: "#0047AB",
              borderRadius: "8px",
              textTransform: "none",
              border: "1px solid #F4F0FF",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#ece9fd",
              },
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleConfirm}
            disabled={deleteScheduleLoading}
            sx={{
              backgroundColor: "#0047AB",
              color: "#fff",
              borderRadius: "8px",
              textTransform: "none",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#002f8c",
              },
            }}
          >
            {deleteScheduleLoading ? "Deleting..." : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>

      <SchedulePostModal open={open} onClose={() => setOpen(false)} />

      <ResetPasswordModal
        open={openResetPassword}
        onClose={() => setOpenResetPassword(false)}
      />
      <Dialog open={platformToggler} onClose={() => setPlatformToggler(false)}>
        <DialogTitle>
          Platform Connections
          <IconButton
            onClick={() => setPlatformToggler(false)}
            style={{ position: "absolute", right: 10, top: 10 }}
          >
            <Close />
          </IconButton>
        </DialogTitle>

        <DialogContent>
          {isDisconnect ? (
            <>
              Are you sure you want to <strong>disconnect</strong> this
              platform?
            </>
          ) : (
            <>
              Are you sure you want to <strong>connect</strong> this platform?
            </>
          )}
          {platformActionError ? (
            <Typography sx={{ mt: 2 }} color="error">
              {platformActionError}
            </Typography>
          ) : null}
        </DialogContent>

        <DialogActions>
          <Button
            onClick={() => setPlatformToggler(false)}
            disabled={isPlatformActionLoading}
          >
            Cancel
          </Button>
          <Button
            onClick={enablePlatform}
            variant="contained"
            color={isDisconnect ? "error" : "primary"}
            disabled={isPlatformActionLoading}
          >
            {isPlatformActionLoading
              ? "Please wait..."
              : isDisconnect
              ? "Disconnect"
              : "Connect"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* <Dialog open={platformToggler} onClose={() => setPlatformToggler(false)}>
        <DialogTitle>
          Platform Connections
          <IconButton
            onClick={() => setPlatformToggler(false)}
            style={{ position: "absolute", right: 10, top: 10 }}
          >
            <Close />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          Are you sure you want to Connect this platform ?
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPlatformToggler(false)}>Cancel</Button>
          <Button onClick={enablePlatform} variant="contained" color="primary">
            Confirm
          </Button>
        </DialogActions>
      </Dialog> */}

      {/* Edit/View Schedule Modal */}
      <EditScheduleModal
        open={editModalOpen}
        onClose={() => {
          setEditModalOpen(false);
          setSelectedSchedulePost(null);
        }}
        scheduleData={selectedSchedulePost}
        onSave={handleEditSave}
      />

      {/* Reschedule Modal */}
      <RescheduleModal
        open={rescheduleModalOpen}
        onClose={() => {
          setRescheduleModalOpen(false);
          setSelectedSchedulePost(null);
        }}
        scheduleData={selectedSchedulePost}
        onSave={handleRescheduleSave}
      />
    </Card>
  );
};

export default Settings;
