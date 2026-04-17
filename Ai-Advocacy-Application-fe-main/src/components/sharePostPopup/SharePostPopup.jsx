import React, { useEffect, useRef, useState } from "react";
import {
  Dialog,
  DialogContent,
  Grid,
  Box,
  Typography,
  Button,
  Avatar,
  TextField,
  Switch,
  IconButton,
  CircularProgress,
  InputAdornment,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import star from "../../assets/points.svg";
import edit from "../../assets/pen.png";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import NavigateBeforeIcon from "@mui/icons-material/NavigateBefore";
import insta from "../../assets/instaf.png";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import fb from "../../assets/f.png";
import twit from "../../assets/twitf.png";
import linkedin from "../../assets/linkedin.png";
import noImage from "../../assets/noimage.jpg";
import CleanCaptionInput from "./CaptionInput";
import HashtagInput from "./HashTagInput";
import EditorBox from "../Layout/Postcreation/Editor";
import {
  createXExternalPost,
  createFacebookExternalPost,
  createLinkedinExternalPost,
  createInstagramExternalPost,
  scheduleExternalPost,
  postSummary,
} from "../../services/postService";
import { toast } from "react-toastify";
import CategoryListing from "./categoriesList";
import { Auth } from "../../contexts/AuthContext";
import {
  clampPlatformContent,
  composePlatformContent,
} from "../../shared/businessPageComposer";
import {
  getBunnyEmbedUrl,
  getResolvedImageUrl,
  getResolvedPosterUrl,
  getResolvedVideoUrl,
  isBunnyStreamMedia,
} from "../../shared/mediaResolver";

const getSystemHashtags = (post = {}) => {
  const candidates = [post?.hashtags, post?.hashTags, post?.postHashtags];
  const source = candidates.find((value) => Array.isArray(value)) || [];
  return source
    .map((tag) => {
      if (typeof tag === "string") {
        return tag.trim();
      }
      return (
        tag?.tag ||
        tag?.name ||
        tag?.value ||
        tag?.hashtag ||
        ""
      ).trim();
    })
    .filter(Boolean);
};

const SharePostPopup = ({
  open,
  onClose,
  type,
  title,
  postReviewData,
  clear,
  selected,
  summary,
}) => {
  // Feature 6: LinkedIn only for Payoneer rollout
  const platFormNewData = ["Linkedin"];
  const isAllSelected = selected?.includes("All");
  const [platformData, setPlatformData] = useState(() => {
    const initialData = {};
    platFormNewData.forEach((platform) => {
      const keyMap = {
        Instagram: "INSTAGRAM",
        Facebook: "FACEBOOK",
        Linkedin: "LINKEDIN",
        "Twitter/X": "X",
      };

      initialData[platform] = {
        enabled: isAllSelected || selected?.includes(platform),
        thoughts: postReviewData?.preparedPost?.content || "",
        hashtags: [],
        showEditor: false,
        aiResult: "",
        caption:
          type === "post"
            ? postReviewData?.platformContentMap?.[keyMap[platform]]
            : summary,
        des: postReviewData?.platformContentMap?.[keyMap[platform]],
        points:
          postReviewData?.estimatedLoyaltyPoints?.[
            platform.toLowerCase() + "Points"
          ] || "",
      };
    });
    return initialData;
  });

  const [thoughts, setThoughts] = useState("");

  const [showEditor, setShowEditor] = useState(false);
  const { businessPages } = Auth();
  const [loader, setLoader] = useState(false);
  const systemHashtags = getSystemHashtags(postReviewData?.preparedPost);

  const handleThoughtChange = (platformName, value) => {
    const nextValue = clampPlatformContent(value, platformName, businessPages, systemHashtags);
    setPlatformData((prev) => ({
      ...prev,
      [platformName]: {
        ...prev[platformName],
        thoughts: nextValue,
      },
    }));
    setThoughts(nextValue);
  };
  const handleCaptionChange = (platformName, value) => {
    const nextValue = clampPlatformContent(value, platformName, businessPages, systemHashtags);
    setPlatformData((prev) => ({
      ...prev,
      [platformName]: {
        ...prev[platformName],
        caption: nextValue,
      },
    }));
  };

  const handleToggleEditor = (platformName) => {
    setPlatformData((prev) => ({
      ...prev,
      [platformName]: {
        ...prev[platformName],
        showEditor: !prev[platformName].showEditor,
      },
    }));
    aiCaptionApiCall(platformName);
  };
  const [aiLoader, setAiLoader] = useState({});
  const [dots, setDots] = useState(".");
  useEffect(() => {
    let interval;
    if (aiLoader) {
      interval = setInterval(() => {
        setDots((prev) => (prev.length >= 3 ? "." : prev + "."));
      }, 1000);
    } else {
      setDots(".");
    }
    return () => clearInterval(interval);
  }, [aiLoader]);
  const aiCaptionApiCall = async (platformName) => {
    try {
      const xComposition = composePlatformContent("", "Twitter/X", businessPages, systemHashtags);
      const payload = {
        postId: postReviewData?.preparedPost?.id,
        userText: thoughts,
        xMaxCharacters: xComposition.maxBaseCharacters || 200,
      };
      setAiLoader((prev) => ({ ...prev, [platformName]: true }));
      const response = await postSummary(payload);
      if (response?.status === true) {
        const summarizedContent =
          response?.generateContentResponseDTO?.summarizedContent || "";
        const xSummarizedContent =
          response?.generateContentResponseDTO?.xsummarizedContent || "";

        // 👇 Update all platform captions
        setPlatformData((prevData) => {
          const updatedData = { ...prevData };
          Object.keys(updatedData).forEach((platform) => {
            // Use xsummarizedContent for Twitter/X, summarizedContent for others
            const contentToUse = platform === "Twitter/X" ? xSummarizedContent : summarizedContent;
            updatedData[platform] = {
              ...updatedData[platform],
              thoughts: contentToUse,
            };
          });
          return updatedData;
        });
      }
    } catch (error) {
      console.log(error);
    } finally {
      setAiLoader((prev) => ({ ...prev, [platformName]: false }));
    }
  };

  const handleHashtagsChange = (platformName, tags) => {
    setPlatformData((prev) => ({
      ...prev,
      [platformName]: {
        ...prev[platformName],
        hashtags: tags,
      },
    }));
  };

  const getPlatformComposition = (platformName, content) =>
    composePlatformContent(content || "", platformName, businessPages, systemHashtags);

  const getPlatformDisplayLimit = (platformName) => {
    const composition = getPlatformComposition(platformName, "");
    return composition.platformKey === "x"
      ? composition.maxBaseCharacters
      : composition.limit;
  };

  const getPlatformLimitNote = (platformName) => {
    const composition = getPlatformComposition(platformName, "");
    if (composition.platformKey !== "x" || !composition.reservedLength) {
      return "";
    }
    return `${composition.reservedLength} characters reserved for your business handle/link`;
  };

  const platforms = [
    {
      name: "Instagram",
      icon: insta,
      maxChar: "2,200",
      maxTags: 30,
      sampleImages: postReviewData?.preparedPost?.media,
      points: postReviewData?.estimatedLoyaltyPoints?.instagramPoints || "",
    },
    {
      name: "Facebook",
      icon: fb,
      maxChar: "63,204",
      sampleImages: postReviewData?.preparedPost?.media,
      points: postReviewData?.estimatedLoyaltyPoints?.facebookPoints || "",
    },
    {
      name: "Linkedin",
      icon: linkedin,
      maxChar: "3,000",
      sampleImages: postReviewData?.preparedPost?.media,
      points: postReviewData?.estimatedLoyaltyPoints?.linkedinPoints || "",
    },
    {
      name: "Twitter/X",
      icon: twit,
      maxChar: String(getPlatformDisplayLimit("Twitter/X")),
      sampleImages: postReviewData?.preparedPost?.media,
      points: postReviewData?.estimatedLoyaltyPoints?.xpoints || "",
      note: getPlatformLimitNote("Twitter/X"),
    },
  ];

  const [currentIndexes, setCurrentIndexes] = useState(platforms.map(() => 0));
  const platformCount = platforms.length;

  const handleImageChange = (platformIndex, direction) => {
    setCurrentIndexes((prevIndexes) => {
      const updatedIndexes = [...prevIndexes];
      const totalImages = platforms[platformIndex].sampleImages.length;
      updatedIndexes[platformIndex] =
        (updatedIndexes[platformIndex] + direction + totalImages) % totalImages;
      return updatedIndexes;
    });
    setActiveBunnyPlayers((prev) => {
      const next = { ...prev };
      delete next[platformIndex];
      return next;
    });
  };

  const platformKeyMap = {
    "Twitter/X": "x",
    Facebook: "facebook",
    Instagram: "instagram",
    Linkedin: "linkedin",
  };

  const isBusinessPageConnection = (platform) => {
    const key = platformKeyMap[platform];
    return key && businessPages?.[key]?.oauthSourcePage === "BUSINESS";
  };

  const handlePost = async () => {
    try {
      setLoader(true);
      const apiMap = {
        "Twitter/X": createXExternalPost,
        Facebook: createFacebookExternalPost,
        Instagram: createInstagramExternalPost,
        Linkedin: createLinkedinExternalPost,
      };

      // Check if any platform is enabled
      const enabledPlatforms = Object.entries(platformData).filter(
        ([_, data]) => data.enabled
      );

      if (enabledPlatforms.length === 0) {
        toast.warning("Please select at least one platform to post.");
        setLoader(false);
        return;
      }

      // Track success/failure
      let successCount = 0;
      let failedPlatforms = [];

      for (const [platform, data] of Object.entries(platformData)) {
        if (data.enabled) {
          if (isBusinessPageConnection(platform)) {
            toast.error(`${platform} is connected as a business page. Please connect your personal account to post.`);
            failedPlatforms.push(platform);
            continue;
          }
          try {
            const composition = getPlatformComposition(platform, data.caption || "");
            if (composition.isSuffixTooLong || composition.isOverLimit) {
              toast.error(
                composition.platformKey === "x"
                  ? `${platform} content exceeds the allowed character limit after adding your business handle/link.`
                  : `${platform} content exceeds the allowed character limit.`
              );
              failedPlatforms.push(platform);
              continue;
            }
            const payload = {
              postId: postReviewData?.preparedPost?.id,
              type: "POST",
              content: composition.finalText,
            };
            const response = await apiMap[platform](payload);
            if (response?.status === true) {
              toast.success(`${platform} post created successfully`);
              successCount++;
            } else {
              toast.error(response?.message || `${platform} post failed. Please check if the platform is linked.`);
              failedPlatforms.push(platform);
            }
          } catch (platformError) {
            toast.error(platformError.message || `${platform} post failed. Please try again.`);
            failedPlatforms.push(platform);
          }
        }
      }

      // Only clear and close on success
      if (successCount > 0) {
        clear();
        onClose();
      }

      // Summary message if partial failure
      if (failedPlatforms.length > 0 && successCount > 0) {
        toast.warning(`Some platforms failed: ${failedPlatforms.join(", ")}`);
      }

    } catch (error) {
      console.log(error);
      toast.error(error.message || "Something went wrong");
    } finally {
      setLoader(false);
    }
  };
  // Feature 5: Open LinkedIn natively with pre-filled text
  const handleLinkedInNativeShare = () => {
    const content = platformData["Linkedin"]?.thoughts || postReviewData?.preparedPost?.content || "";
    const text = encodeURIComponent(content.slice(0, 3000));
    const url = `https://www.linkedin.com/sharing/share-offsite/?text=${text}`;
    window.open(url, "_blank", "noopener,noreferrer,width=600,height=600");
    toast.success("LinkedIn opened with pre-filled content. Review and publish!");
    clear();
    onClose();
  };

  const handleShare = async () => {
    // Feature 4: If post is locked (non-editable), skip editing and go straight to native share
    const isEditable = postReviewData?.preparedPost?.isEditable !== false;
    if (!isEditable) {
      handleLinkedInNativeShare();
      return;
    }

    // Feature 5: Always use native LinkedIn share (opens LinkedIn compose page)
    handleLinkedInNativeShare();
    return;

    // eslint-disable-next-line no-unreachable
    try {
      setLoader(true);
      const apiMap = {
        "Twitter/X": createXExternalPost,
        Facebook: createFacebookExternalPost,
        Instagram: createInstagramExternalPost,
        Linkedin: createLinkedinExternalPost,
      };

      // Check if any platform is enabled
      const enabledPlatforms = Object.entries(platformData).filter(
        ([_, data]) => data.enabled
      );

      if (enabledPlatforms.length === 0) {
        toast.warning("Please select at least one platform to share.");
        setLoader(false);
        return;
      }

      // Track success/failure
      let successCount = 0;
      let failedPlatforms = [];

      for (const [platform, data] of Object.entries(platformData)) {
        if (data.enabled) {
          if (isBusinessPageConnection(platform)) {
            toast.error(`${platform} is connected as a business page. Please connect your personal account to share.`);
            failedPlatforms.push(platform);
            continue;
          }
          try {
            const composition = getPlatformComposition(platform, data.thoughts || "");
            if (composition.isSuffixTooLong || composition.isOverLimit) {
              toast.error(
                composition.platformKey === "x"
                  ? `${platform} content exceeds the allowed character limit after adding your business handle/link.`
                  : `${platform} content exceeds the allowed character limit.`
              );
              failedPlatforms.push(platform);
              continue;
            }
            const payload = {
              postId: postReviewData?.preparedPost?.id,
              type: "SHARE",
              content: composition.finalText,
            };
            const response = await apiMap[platform](payload);
            if (response?.status === true) {
              toast.success(`${platform} shared successfully`);
              successCount++;
            } else {
              toast.error(response?.message || `${platform} share failed. Please check if the platform is linked.`);
              failedPlatforms.push(platform);
            }
          } catch (platformError) {
            toast.error(platformError.message || `${platform} share failed. Please try again.`);
            failedPlatforms.push(platform);
          }
        }
      }

      // Only clear and close on success
      if (successCount > 0) {
        clear();
        onClose();
      }

      // Summary message if partial failure
      if (failedPlatforms.length > 0 && successCount > 0) {
        toast.warning(`Some platforms failed: ${failedPlatforms.join(", ")}`);
      }

    } catch (error) {
      console.log(error);
      toast.error(error.message || "Something went wrong");
    } finally {
      setLoader(false);
    }
  };
  const handleGenerateClick = () => {
    setShowEditor(!showEditor);
  };

  const handleTogglePlatform = (platformName) => {
    setPlatformData((prev) => ({
      ...prev,
      [platformName]: {
        ...prev[platformName],
        enabled: !prev[platformName].enabled,
      },
    }));
  };
  const [schedule, setSchedule] = useState(false);
  const [startDate, setStartDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split("T")[0];
  });
  const [startTime, setStartTime] = useState("12:00");
  const dateInputRef = useRef(null);
  const timeInputRef = useRef(null);

  // Format date for display (e.g., "Jan 8, 2025")
  const formatDateForDisplay = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString + "T00:00:00");
    return date.toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const schedulePost = async () => {
    try {
      setLoader(true);
      const enabledPlatforms = [];
      const platformContents = {};

      for (const [platform, data] of Object.entries(platformData)) {
        if (data.enabled) {
          enabledPlatforms.push(platform.toUpperCase());
          platformContents[platform] = data.caption;
        }
      }

      // Combine date + time into ISO format
      const combinedDateTime = new Date(`${startDate}T${startTime}:00Z`);

      const payload = {
        postId: postReviewData?.preparedPost?.id,
        type: "POST",
        content: postReviewData?.preparedPost?.content || "",
        platforms: enabledPlatforms,
        scheduledTimeUtc: combinedDateTime.toISOString().split(".")[0] + "Z",
      };

      const response = await scheduleExternalPost(payload);
      if (response?.status === true) {
        toast.success("Post scheduled successfully");
        onClose();
        clear();
      } else {
        toast.error("Failed to schedule post");
      }
    } catch (error) {
      console.error("❌ Error scheduling post:", error);
      toast.error("Something went wrong while scheduling");
    } finally {
      setLoader(false);
    }
  };

  // const [schedule, setSchedule] = useState(false);
  // const [startDate, setStartDate] = useState("2025-05-12");
  // const dateInputRef = useRef(null);
  // const schedulePost = async () => {
  //   try {
  //     setLoader(true);
  //     const enabledPlatforms = [];
  //     const platformContents = {};
  //     for (const [platform, data] of Object.entries(platformData)) {
  //       if (data.enabled) {
  //         enabledPlatforms.push(platform.toUpperCase());
  //         platformContents[platform] = data.caption;
  //       }
  //     }
  //     const payload = {
  //       postId: postReviewData?.preparedPost?.id,
  //       type: "POST",
  //       content: "",
  //       platforms: enabledPlatforms,
  //       scheduledTimeUtc: startDate
  //         ? new Date(startDate).toISOString().split(".")[0] + "Z"
  //         : new Date().toISOString().split(".")[0] + "Z",
  //     };
  //     const response = await scheduleExternalPost(payload);
  //     if (response?.status === true) {
  //       toast.success("Post scheduled successfully");
  //       onClose();
  //       clear();
  //     } else {
  //       toast.error("Failed to schedule post");
  //     }
  //   } catch (error) {
  //     console.error("❌ Error scheduling post:", error);
  //     toast.error("Something went wrong while scheduling");
  //   } finally {
  //     setLoader(false);
  //   }
  // };

  const handleCloseSchedule = () => {
    setSchedule(false);
  };

  const handleCloseMainPopup = () => {
    clear();
    onClose();
  };

  const [playingIndex, setPlayingIndex] = useState(null);
  const [activeBunnyPlayers, setActiveBunnyPlayers] = useState({});
  const videoRefs = useRef({});

  useEffect(() => {
    setPlayingIndex(null);
    setActiveBunnyPlayers({});
    setCurrentIndexes(Array.from({ length: platformCount }, () => 0));
  }, [open, platformCount, postReviewData?.preparedPost?.id]);

  const handlePlayToggle = (index) => {
    const video = videoRefs.current[index];
    if (!video) return;

    if (playingIndex === index) {
      video.pause();
      setPlayingIndex(null);
    } else {
      // Pause other videos
      Object.values(videoRefs.current).forEach((v) => v && v.pause());
      video.play();
      setPlayingIndex(index);
    }
  };

  const handleActivateBunnyPlayer = (platformIndex) => {
    setActiveBunnyPlayers((prev) => ({
      ...prev,
      [platformIndex]: true,
    }));
  };
  return (
    <>
      <Dialog
        elevation={0}
        open={schedule}
        maxWidth="sm"
        PaperProps={{
          sx: {
            width: { xs: "90vw", sm: "500px", md: "600px" },
            p: 0,
            m: 0,
            borderRadius: 2,
          },
        }}
        onClose={handleCloseSchedule}
      >
        <Box sx={{ backgroundColor: "#f6f3ff", height: "auto", pb: 2 }}>
          {/* Header */}
          <Box
            bgcolor={"#ffffff"}
            boxShadow={"0px 4px 20px rgba(236, 236, 236, 0.25)"}
            display="flex"
            padding={2}
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography
              variant="h6"
              color="rgba(59, 59, 59, 1)"
              fontWeight={500}
            >
              Schedule this Post
            </Typography>
            <IconButton
              sx={{ width: "24px", height: "24px" }}
              onClick={handleCloseSchedule}
            >
              <CloseIcon sx={{ color: "rgba(138, 138, 138, 1)" }} />
            </IconButton>
          </Box>

          {/* Date & Time Fields */}
          <Box display={"flex"} flexWrap={"wrap"}>
            {/* Date */}
            <Grid item xs={6} sx={{ padding: "30px", position: "relative" }}>
              <Typography variant="body2" color="#95919D" mb={1}>
                Select Date
              </Typography>
              <TextField
                fullWidth
                value={formatDateForDisplay(startDate)}
                variant="standard"
                onClick={() => dateInputRef.current?.showPicker()}
                InputProps={{
                  readOnly: true,
                  startAdornment: (
                    <InputAdornment position="start">
                      <label htmlFor="start-date-picker">
                        <CalendarMonthOutlinedIcon
                          sx={{ color: "#1976d2", cursor: "pointer" }}
                        />
                      </label>
                    </InputAdornment>
                  ),
                }}
              />
              <input
                type="date"
                id="start-date-picker"
                ref={dateInputRef}
                value={startDate}
                min={new Date().toISOString().split("T")[0]}
                onChange={(e) => setStartDate(e.target.value)}
                style={{
                  position: "absolute",
                  top: 50,
                  left: 0,
                  opacity: 0,
                  width: 0,
                  height: 0,
                  pointerEvents: "auto",
                }}
              />
            </Grid>

            {/* Time */}
            <Grid item xs={6} sx={{ padding: "30px", position: "relative" }}>
              <Typography variant="body2" color="#95919D" mb={1}>
                Select Time
              </Typography>
              <TextField
                fullWidth
                value={startTime}
                variant="standard"
                onClick={() => timeInputRef.current?.showPicker()}
                InputProps={{
                  readOnly: true,
                  startAdornment: (
                    <InputAdornment position="start">
                      <label htmlFor="start-time-picker">
                        <AccessTimeIcon
                          sx={{ color: "#1976d2", cursor: "pointer" }}
                        />
                      </label>
                    </InputAdornment>
                  ),
                }}
              />
              <input
                type="time"
                id="start-time-picker"
                ref={timeInputRef}
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                style={{
                  position: "absolute",
                  top: 50,
                  left: 0,
                  opacity: 0,
                  width: 0,
                  height: 0,
                  pointerEvents: "auto",
                }}
              />
            </Grid>
          </Box>

          {/* Footer Buttons */}
          <Box
            sx={{
              display: "flex",
              justifyContent: "flex-end",
              gap: 2,
              px: 3,
            }}
          >
            <Button
              variant="outlined"
              color="secondary"
              onClick={handleCloseSchedule}
              disabled={loader}
              sx={{ textTransform: "none" }}
            >
              Cancel
            </Button>
            {loader ? (
              <CircularProgress size="30px" />
            ) : (
              <Button
                variant="contained"
                onClick={schedulePost}
                sx={{
                  backgroundColor: "#2D76DC",
                  textTransform: "none",
                  "&:hover": { backgroundColor: "#1c5db8" },
                }}
              >
                Schedule
              </Button>
            )}
          </Box>
        </Box>
      </Dialog>

      <Dialog
        elevation={0}
        open={open}
        PaperProps={{ sx: { p: 0, m: 0, borderRadius: 0 } }}
        onClose={handleCloseMainPopup}
        fullScreen
      >
        <Box sx={{ backgroundColor: "#f6f3ff", height: "auto", pb: 7 }}>
          <Box
            bgcolor={"#ffffff"}
            boxShadow={"0px 4px 20px rgba(236, 236, 236, 0.25)"}
            display="flex"
            sx={{ pl: "50px", pr: "20px", pt: "13px", pb: "13px" }}
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography
              variant="h6"
              color="rgba(59, 59, 59, 1)"
              fontWeight={500}
            >
              {title}
            </Typography>
            <IconButton
              sx={{ width: "24px", height: "24px" }}
              onClick={handleCloseMainPopup}
            >
              <CloseIcon sx={{ color: "rgba(138, 138, 138, 1)" }} />
            </IconButton>
          </Box>

          <DialogContent sx={{ pl: 6, pr: 6, pt: "10px" }}>
            <Box
              bgcolor={"#FFFFFF"}
              sx={{ padding: "15px 25px", borderRadius: "16px" }}
              display="flex"
              alignItems="center"
              justifyContent={"space-between"}
              mb={1.2}
            >
              <Box display={"flex"} gap={1} alignItems={"center"}>
                <Typography
                  variant="subtitle2"
                  color="#95919D"
                  fontWeight={500}
                  fontSize={"12px"}
                >
                  Post By,
                </Typography>
                <Typography
                  variant="subtitle2"
                  fontSize={"14px"}
                  color="#3B3B3B"
                  fontWeight={500}
                >
                  @{postReviewData?.preparedPost?.createdBy?.name || ""}
                </Typography>
                <CategoryListing postReviewData={postReviewData} />
                {/* <Box display="flex" gap={1}>
                {postReviewData?.preparedPost?.categories?.length > 0 &&
                  postReviewData?.preparedPost?.categories?.map((category) => (
                    <Box
                    key={category.id}
                      bgcolor={"#E7F1FF"}
                      borderRadius={"4px"}
                      padding={"6px 8px"}
                      border={"0.6px solid #2D76DC"}
                    >
                      <Typography fontSize={"10px"} fontWeight={400}>
                        {category?.categoryName}
                      </Typography>
                    </Box>
                  ))}
              </Box> */}
              </Box>
              <Box>
                <Typography
                  align="right"
                  color="#3B3B3B"
                  fontSize={"14px"}
                  fontWeight={400}
                  display={"flex"}
                  alignItems={"center"}
                  gap={"5px"}
                >
                  Share to all & earn:{" "}
                  <img src={star} width={"20px"} height={"20px"} />
                  <strong style={{ color: "#8296B2", fontSize: "15px" }}>
                    {postReviewData?.estimatedLoyaltyPoints?.total || ""}
                  </strong>
                  <Button
                    sx={{
                      ml: "3px",
                      backgroundColor: "#f6f1ff",
                      color: "#0047AB",
                      textTransform: "none",
                      borderRadius: 2,
                      px: 1.5,
                      fontWeight: 500,
                      fontSize: "12px",
                    }}
                    onClick={handleCloseMainPopup}
                    disabled={loader}
                  >
                    Cancel
                  </Button>
                  {/* {type === "post" && ( */}
                  <Button
                    sx={{
                      ml: "3px",
                      backgroundColor: "#f6f1ff",
                      color: "#0047AB",
                      textTransform: "none",
                      borderRadius: 2,
                      px: 1.5,
                      fontWeight: 500,
                      fontSize: "12px",
                    }}
                    onClick={() => setSchedule(true)}
                    disabled={loader}
                  >
                    Schedule
                  </Button>
                  {/* )} */}
                  {loader ? (
                    <CircularProgress size="30px" />
                  ) : (
                    <>
                      {type === "post" ? (
                        <Button
                          sx={{
                            ml: "3px",
                            backgroundColor: "#0047AB",
                            textTransform: "none",
                            borderRadius: 2,
                            px: 2,
                            color: "#fff",
                            fontSize: "12px",
                          }}
                          onClick={handlePost}
                          disabled={loader}
                        >
                          Post
                        </Button>
                      ) : (
                        <Button
                          sx={{
                            ml: "3px",
                            backgroundColor: "#0047AB",
                            textTransform: "none",
                            borderRadius: 2,
                            px: 2,
                            color: "#fff",
                            fontSize: "12px",
                          }}
                          onClick={handleShare}
                          disabled={loader}
                        >
                          Share
                        </Button>
                      )}{" "}
                    </>
                  )}
                </Typography>
              </Box>
            </Box>

            <Grid container spacing={1.2}>
              {platforms?.map((platform, index) => {
                const currentMedia =
                  platform?.sampleImages?.[currentIndexes[index]] || {};
                const isVideo = currentMedia?.mediaType === "VIDEO";
                const isBunnyVideo = isBunnyStreamMedia(currentMedia);
                return (
                  <Grid item xs={12} md={6} key={index}>
                    {platformData[platform.name]?.enabled ? (
                      <Box
                        sx={{
                          p: 2,
                          backgroundColor: "#fff",
                          borderRadius: 3,
                          display: "flex",
                          gap: 1,
                          boxShadow: "0px 4px 20px rgba(236, 236, 236, 0.25)",
                        }}
                      >
                        {platform?.sampleImages?.length > 0 && (
                          <Box
                            sx={{
                              width: "220px",
                              height: "235px",
                              borderRadius: 3,
                              overflow: "hidden",
                              position: "relative",
                            }}
                          >
                            {isVideo ? (
                              <Box
                                sx={{
                                  position: "relative",
                                  width: "100%",
                                  height: "100%",
                                }}
                              >
                                {isBunnyVideo ? (
                                  activeBunnyPlayers?.[index] ? (
                                    <iframe
                                      src={getBunnyEmbedUrl(
                                        getResolvedVideoUrl(currentMedia)
                                      )}
                                      title={platform.name}
                                      style={{
                                        width: "100%",
                                        height: "100%",
                                        border: 0,
                                        display: "block",
                                        borderRadius: 12,
                                        background: "#000",
                                      }}
                                      allow="accelerometer; gyroscope; encrypted-media; picture-in-picture;"
                                      allowFullScreen
                                    />
                                  ) : (
                                    <>
                                      <img
                                        src={getResolvedPosterUrl(
                                          currentMedia,
                                          getResolvedImageUrl(currentMedia, noImage)
                                        )}
                                        alt="preview"
                                        style={{
                                          width: "100%",
                                          height: "100%",
                                          objectFit: "cover",
                                          borderRadius: 12,
                                          background: "#000",
                                        }}
                                      />
                                      <IconButton
                                        onClick={() =>
                                          handleActivateBunnyPlayer(index)
                                        }
                                        sx={{
                                          position: "absolute",
                                          top: "50%",
                                          left: "50%",
                                          transform: "translate(-50%, -50%)",
                                          background: "rgba(0,0,0,0.5)",
                                          color: "#fff",
                                          fontSize: "15px",
                                          padding: "6px 8px 5px 12px",
                                          "&:hover": {
                                            background: "rgba(0,0,0,0.7)",
                                          },
                                        }}
                                      >
                                        ▶
                                      </IconButton>
                                    </>
                                  )
                                ) : (
                                  <>
                                    <video
                                      ref={(el) => (videoRefs.current[index] = el)}
                                      src={getResolvedVideoUrl(currentMedia)}
                                      poster={getResolvedPosterUrl(currentMedia)}
                                      style={{
                                        width: "100%",
                                        height: "100%",
                                        objectFit: "cover",
                                        borderRadius: 12,
                                        background: "#000",
                                      }}
                                      onPause={() => setPlayingIndex(null)}
                                    />
                                    {playingIndex !== index && (
                                      <IconButton
                                        onClick={() => handlePlayToggle(index)}
                                        sx={{
                                          position: "absolute",
                                          top: "50%",
                                          left: "50%",
                                          transform: "translate(-50%, -50%)",
                                          background: "rgba(0,0,0,0.5)",
                                          color: "#fff",
                                          fontSize: "15px",
                                          padding: "6px 8px 5px 12px",
                                          "&:hover": {
                                            background: "rgba(0,0,0,0.7)",
                                          },
                                        }}
                                      >
                                        ▶
                                      </IconButton>
                                    )}
                                  </>
                                )}
                              </Box>
                            ) : (
                              <img
                                src={getResolvedImageUrl(currentMedia, noImage)}
                                alt="preview"
                                style={{
                                  width: "100%",
                                  height: "100%",
                                  objectFit: "cover",
                                  borderRadius: 12,
                                }}
                              />
                            )}
                            {platform?.sampleImages?.length > 1 && (
                              <>
                                <IconButton
                                  sx={{
                                    position: "absolute",
                                    width: "18px",
                                    height: "18px",
                                    borderRadius: "50%",
                                    bottom: "3%",
                                    left: 5,
                                    background: "#FFFFFF",
                                  }}
                                  onClick={() => handleImageChange(index, -1)}
                                >
                                  <NavigateBeforeIcon
                                    sx={{ color: "#2D76DC", fontSize: "10px" }}
                                  />
                                </IconButton>

                                {/* Next Button */}
                                <IconButton
                                  sx={{
                                    position: "absolute",
                                    width: "18px",
                                    height: "18px",
                                    borderRadius: "50%",
                                    bottom: "3%",
                                    right: 5,
                                    background: "#FFFFFF",
                                  }}
                                  onClick={() => handleImageChange(index, 1)}
                                >
                                  <NavigateNextIcon
                                    sx={{ color: "#2D76DC", fontSize: "10px" }}
                                  />
                                </IconButton>

                                {/* Dots */}
                                <Box
                                  sx={{
                                    display: "flex",
                                    justifyContent: "center",
                                    gap: 0.8,
                                    position: "absolute",
                                    bottom: 13,
                                    left: 0,
                                    right: 0,
                                  }}
                                >
                                  {platform?.sampleImages?.map((_, dotIndex) => (
                                    <Box
                                      key={dotIndex}
                                      sx={{
                                        width: 5,
                                        height: 5,
                                        borderRadius: "50%",
                                        backgroundColor:
                                          dotIndex === currentIndexes[index]
                                            ? "#0047AB"
                                            : "#ccc",
                                      }}
                                    />
                                  ))}
                                </Box>
                              </>
                            )}

                            {/* Update Button */}
                            <Button
                              fullWidth
                              sx={{
                                mt: 1,
                                backgroundColor: "#f5f1ff",
                                color: "#3B3B3B",
                                borderRadius: 2,
                                fontSize: "16px",
                                fontWeight: 500,
                                textTransform: "none",
                                gap: 1,
                                py: 1,
                              }}
                              startIcon={
                                <img
                                  src={edit}
                                  width={"16px"}
                                  height={"16px"}
                                  alt="edit"
                                />
                              }
                            >
                              Update Image
                            </Button>
                          </Box>
                        )}

                        {/* Right Content */}
                        <Box
                          sx={{
                            flex: 1,
                          }}
                        >
                          <Box
                            display="flex"
                            justifyContent="space-between"
                            alignItems="center"
                          >
                            <Box display="flex" alignItems="center" gap={1}>
                              <Box
                                sx={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: "5px",
                                  backgroundColor: "#2D76DC",
                                  borderRadius: "0px 20px 20px 0px",
                                  pr: 2,
                                  pl: 1,
                                  py: 0.5,
                                  color: "#fff",
                                  textTransform: "none",
                                }}
                              >
                                <img
                                  src={platform?.icon}
                                  width={"13px"}
                                  height={"13px"}
                                />
                                <Typography
                                  color="#fff"
                                  fontWeight={500}
                                  fontSize={"12px"}
                                >
                                  {platform?.name}
                                </Typography>
                              </Box>
                              <Box display="flex" alignItems="center" gap={0.5}>
                                <img
                                  src={star}
                                  width={"15px"}
                                  height={"15px"}
                                />
                                <Typography
                                  color="#8296B2"
                                  fontSize={"13px"}
                                  fontWeight={400}
                                >
                                  {platform?.points || ""}
                                </Typography>
                              </Box>
                            </Box>
                            <Switch
                              size="small"
                              checked={platformData[platform.name]?.enabled}
                              onChange={() =>
                                handleTogglePlatform(platform.name)
                              }
                            />
                          </Box>

                          <Box
                            display="flex"
                            alignItems="center"
                            gap={1}
                            mt={"10px"}
                          >
                            <Avatar
                              src={
                                postReviewData?.preparedPost?.createdBy
                                  ?.profileImageUrl || ""
                              }
                              sx={{ width: "30px", height: "30px" }}
                            />
                            <Typography
                              color="#252525"
                              fontSize={"12px"}
                              fontWeight={500}
                            >
                              @
                              {postReviewData?.preparedPost?.createdBy?.name ||
                                ""}
                            </Typography>
                          </Box>
                          {type !== "post" && (
                            <>
                              <Box
                                sx={{
                                  mt: "10px",
                                  borderRadius: "12px",
                                  backgroundColor: "#f7f8fa",
                                  border: "1px solid #e0e0e0",
                                  px: "8px",
                                  py: "8px",
                                  display: "flex",
                                  flexDirection: "row",
                                  flexWrap: "wrap",
                                  alignItems: "flex-start",
                                  gap: 1,
                                }}
                              >
                                <TextField
                                  variant="standard"
                                  placeholder="Write Ai Caption for this post..."
                                  fullWidth
                                  multiline
                                  value={platformData[platform.name]?.thoughts}
                                  onChange={(e) =>
                                    handleThoughtChange(
                                      platform.name,
                                      e.target.value
                                    )
                                  }
                                  InputProps={{
                                    disableUnderline: true,
                                    sx: {
                                      fontSize: "11px",
                                      flex: 1,
                                      backgroundColor: "transparent",
                                    },
                                  }}
                                  sx={{
                                    flex: 1,
                                    minWidth: "200px",
                                    ".MuiInputBase-root": {
                                      backgroundColor: "transparent",
                                    },
                                  }}
                                />
                                {aiLoader[platform.name] ? (
                                  <Button
                                    variant="contained"
                                    disabled
                                    sx={{
                                      background:
                                        "linear-gradient(to right, #BC77F4, #0047AB)",
                                      color: "#fff",
                                      borderRadius: "8px",
                                      textTransform: "none",
                                      padding: "4px 10px",
                                      boxShadow: "none",
                                      fontSize: "12px",
                                      fontWeight: 500,
                                      mt: "auto",
                                      "&:hover": {
                                        background:
                                          "linear-gradient(to right, #8a6be9, #306ddf)",
                                        boxShadow: "none",
                                      },
                                    }}
                                  >
                                    ✨ Generate{dots}
                                  </Button>
                                ) : (
                                  <Button
                                    variant="contained"
                                    sx={{
                                      background:
                                        "linear-gradient(to right, #BC77F4, #0047AB)",
                                      color: "#fff",
                                      borderRadius: "8px",
                                      textTransform: "none",
                                      padding: "4px 10px",
                                      boxShadow: "none",
                                      fontSize: "12px",
                                      fontWeight: 500,
                                      mt: "auto",
                                      "&:hover": {
                                        background:
                                          "linear-gradient(to right, #8a6be9, #306ddf)",
                                        boxShadow: "none",
                                      },
                                    }}
                                    onClick={() =>
                                      handleToggleEditor(platform.name)
                                    }
                                  >
                                    ✨ Generate
                                  </Button>
                                )}
                              </Box>
                              <Typography
                                textAlign={"end"}
                                variant="body2"
                                color={
                                  getPlatformComposition(
                                    platform.name,
                                    platformData[platform.name]?.thoughts
                                  ).remainingCharacters === 0
                                    ? "#DA4040"
                                    : "#ADA7A7"
                                }
                                fontSize={"10px"}
                                mt={1}
                              >
                                {(() => {
                                  const composition = getPlatformComposition(
                                    platform.name,
                                    platformData[platform.name]?.thoughts
                                  );
                                  return `Remaining: ${composition.remainingCharacters} / ${composition.maxBaseCharacters}`;
                                })()}
                              </Typography>
                            </>
                          )}
                          {(() => {
                            const contentValue =
                              type === "post"
                                ? platformData[platform.name]?.caption
                                : platformData[platform.name]?.thoughts;
                            const composition = getPlatformComposition(
                              platform.name,
                              contentValue
                            );

                            if (composition.suffixMode === "tag") {
                              return (
                                <Typography
                                  variant="body2"
                                  color="#8296B2"
                                  fontSize={"11px"}
                                  mt={1}
                                >
                                  Business page tag will be added: {composition.suffixText}
                                </Typography>
                              );
                            }

                            if (composition.suffixMode === "url") {
                              return (
                                <Typography
                                  variant="body2"
                                  color="#8296B2"
                                  fontSize={"11px"}
                                  mt={1}
                                >
                                  Business page URL will be added: {composition.suffixText}
                                </Typography>
                              );
                            }

                            return (
                              <Typography
                                variant="body2"
                                color="#DA4040"
                                fontSize={"11px"}
                                mt={1}
                              >
                                No business page metadata found for this platform.
                              </Typography>
                            );
                          })()}

                          {/* {platformData[platform.name]?.showEditor && (
                            <Box>
                              <Typography
                                variant="subtitle1"
                                sx={{
                                  mb: 0.4,
                                  mt: 1,
                                  fontWeight: 400,
                                  color: "#9C9C9C",
                                  fontSize: "12px",
                                }}
                              >
                                Result Generated
                              </Typography>
                              <EditorBox />
                            </Box>
                          )} */}
                          {/* {type !== "post" && (
                            <Box
                              sx={{
                                mt: "10px",
                                borderRadius: "12px",
                                backgroundColor: "#f7f8fa",
                                border: "1px solid #e0e0e0",
                                px: "8px",
                                py: "8px",
                                display: "flex",
                                flexDirection: "row",
                                flexWrap: "wrap",
                                alignItems: "flex-start",
                                gap: 1,
                                maxHeight: "100px",
                                overflowY: "auto",
                                scrollbarWidth: "none",
                                msOverflowStyle: "none",
                                "&::-webkit-scrollbar": {
                                  display: "none",
                                },
                              }}
                            >
                              <Typography fontSize={"12px"}>
                                {platformData[platform.name]?.des}
                              </Typography>
                            </Box>
                          )} */}
                          {type === "post" && (
                            <>
                              <Typography
                                textAlign={"end"}
                                variant="body2"
                                color={
                                  getPlatformComposition(
                                    platform.name,
                                    platformData[platform.name]?.caption
                                  ).remainingCharacters === 0
                                    ? "#DA4040"
                                    : "#ADA7A7"
                                }
                                fontSize={"10px"}
                                mt={1}
                              >
                                {(() => {
                                  const composition = getPlatformComposition(
                                    platform.name,
                                    platformData[platform.name]?.caption
                                  );
                                  return `Remaining: ${composition.remainingCharacters} / ${composition.maxBaseCharacters}`;
                                })()}
                              </Typography>

                              <Box
                                sx={{
                                  maxHeight: "200px",
                                  overflowY: "auto",
                                  scrollbarWidth: "none",
                                  msOverflowStyle: "none",

                                  "&::-webkit-scrollbar": {
                                    display: "none",
                                  },
                                }}
                              >
                                <CleanCaptionInput
                                  value={platformData[platform.name]?.caption}
                                  onChange={(newVal) =>
                                    handleCaptionChange(platform.name, newVal)
                                  }
                                />
                              </Box>
                            </>
                          )}
                          {/* {platform?.name === "Instagram" && (
                          <>
                            <Typography
                              textAlign={"end"}
                              variant="body2"
                              color="#ADA7A7"
                              fontSize={"10px"}
                              mt={1}
                            >
                              Max HashTags: 30
                            </Typography>

                            <HashtagInput
                              value={platformData[platform.name]?.hashtags}
                              onChange={(tags) =>
                                handleHashtagsChange(platform.name, tags)
                              }
                            />
                          </>
                        )} */}
                        </Box>
                      </Box>
                    ) : (
                      <Box
                        sx={{
                          height: "100px",
                          background: "#ffffff",
                          borderRadius: 3,
                          px: 2,
                          // py: 2,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",

                          boxShadow: "0px 4px 20px rgba(236, 236, 236, 0.25)",
                        }}
                      >
                        <Box
                          display="flex"
                          width={"100%"}
                          justifyContent={"space-between"}
                          alignItems="center"
                          gap={1}
                        >
                          <Box display={"flex"} gap={1}>
                            <Box
                              sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: "5px",
                                backgroundColor: "#2D76DC",
                                borderRadius: "0px 20px 20px 0px",
                                pr: 2,
                                pl: 2,
                                py: 1,
                                color: "#fff",
                                textTransform: "none",
                              }}
                            >
                              <img
                                src={platform?.icon}
                                width={"13px"}
                                height={"13px"}
                              />
                              <Typography
                                color="#fff"
                                fontWeight={500}
                                fontSize={"12px"}
                              >
                                {platform?.name}
                              </Typography>
                            </Box>
                            <Box display="flex" alignItems="center" gap={0.5}>
                              <img src={star} width={"20px"} height={"20px"} />
                              <Typography
                                color="#8296B2"
                                fontSize={"16px"}
                                fontWeight={400}
                              >
                                {platform?.points || ""}
                              </Typography>
                            </Box>
                          </Box>

                          <Switch
                            size="small"
                            checked={false}
                            onChange={() => handleTogglePlatform(platform.name)}
                          />
                        </Box>
                      </Box>
                    )}
                  </Grid>
                );
              })}
            </Grid>
          </DialogContent>
        </Box>
      </Dialog>
    </>
  );
};

export default SharePostPopup;
