import React, { useState, useEffect } from "react";
import {
  Card,
  TextField,
  Button,
  Chip,
  Box,
  IconButton,
  Typography,
  Avatar,
  Autocomplete,
  Popper,
  InputAdornment,
  Grid,
  FormControlLabel,
  Checkbox,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Switch,
} from "@mui/material";
import "./Postcreation.css";
import CloseIcon from "@mui/icons-material/Close";
import AddIcon from "@mui/icons-material/Add";
import coins from "../../../assets/Profile-coins.svg";
import message from "../../../assets/Posts.svg";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SearchIcon from "@mui/icons-material/Search";
import {
  createPost,
  preparePost,
  uploadMedia,
} from "../../../services/postService";
import EditorBox from "./Editor";
import ImageLibraryModal from "./ImageUploader";
import VideoLibraryModal from "./videoUploader";
import SharePostPopup from "../../sharePostPopup/SharePostPopup";
import ActivitiesPopup from "../../activities/ActivitiesPopup";
import LoaderOverlay from "./loaderOverLay";
import StatusPopup from "./StatusPopup";
import { toast } from "react-toastify";
import { Auth } from "../../../contexts/AuthContext";
import {
  employeeTrendingListing,
  employeeTrendingTopics,
  employeeUserListing,
} from "../../../services/adminServices";
import { Content_Generation } from "../../../services/ai_post_generation";
import {
  clampPlatformContent,
  composePlatformContent,
} from "../../../shared/businessPageComposer";
const TextEditor = ({ userProfile }) => {
  const [postText, setPostText] = useState("");
  const [categories, setCategories] = useState([]);
  const [categoriesIds, setCategoriesIds] = useState([]);
  // Feature 4: editable toggle
  const [isEditable, setIsEditable] = useState(true);
  const [postReview, setPostReview] = useState(false);
  const [activities, setActivities] = useState(false);
  const [categoryInput, setCategoryInput] = useState("");
  const [trendsInput, setTrendsInput] = useState("");
  const [videos, setVideos] = useState([]);
  const [openVideoModal, setOpenVideoModal] = useState(false);
  const [taggedMembers, setTaggedMembers] = useState([]);
  const [taggedMemberInput, setTaggedMemberInput] = useState("");
  const [taggedMemberIds, setTaggedMemberIds] = useState([]);
  const [isTaggingVisible, setIsTaggingVisible] = useState(false);
  const [activeButton, setActiveButton] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [postData, setPostData] = useState(null);
  const [userData, setUsersData] = useState([]);
  const [trendsData, setTrendsData] = useState([]);
  const [LoggedInUser, setLoggedInUser] = useState(null);
  const [discard, setDiscard] = useState(false);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState("");
  const [images, setImages] = useState([]);
  const { loyaltyPoints, categoriesData, businessPages } = Auth();
  const [uploadedImageIds, setUploadedImageIds] = useState([]);
  const [uploadLibraryId, setUploadLibraryId] = useState([]);
  const [selectedTrends, setSelectedTrends] = useState([]);
  const [selectedTrendingTopic, setSelectedTrendingTopic] = useState("");
  useEffect(() => {
    const storedUser = localStorage.getItem("userProfile");
    if (storedUser) {
      setLoggedInUser(JSON.parse(storedUser));
    }
  }, []);

  const addTrends = (newValue) => {
    if (!newValue) {
      setTrendsInput("");
      return;
    }
    const tags = String(newValue)
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);
    if (tags.length === 0) {
      setTrendsInput("");
      return;
    }
    setSelectedTrends((prev) => {
      const next = [...prev];
      for (const tag of tags) {
        if (!next.includes(tag)) next.push(tag);
      }
      return next;
    });
    setTrendsInput("");
  };
  // 🔹 Remove a selected trend
  const removeTrends = (trend) => {
    setSelectedTrends((prev) => prev.filter((t) => t !== trend));
  };

  const clear = () => {
    setPostText("");
    setCategories([]);
    setTaggedMembers([]);
    setCategoryInput("");
    setVideos([]);
    setImages([]);
    setIsTaggingVisible(false);
    setEditorValue("");
    setXGeneratedContent(null);
    setPostReviewData(null);
    setTaggedMemberIds([]);
    setCategoriesIds([]);
    setUploadedImageIds([]);
    setUploadLibraryId([]);
    setSelectedTrends([]);
    setSelectedTrendingTopic("");
  };

  const [postId, setPostId] = useState(null);

  const handlePost = async () => {
    // Validate platform selection
    if (selected.length === 0) {
      toast.warning("Please select at least one social media platform to post");
      return;
    }

    try {
      setLoading(true);
      setLoadingMessage("Creating post...");

      // Minimum 1.5 second loader duration for testing
      await new Promise((resolve) => setTimeout(resolve, 1500));

      if (!editorValue.trim()) {
        console.error("Content is required.");
        toast.error("Content is required.");
        setLoading(false);
        setLoadingMessage("");
        return;
      }
      const postData = {
        content: editorValue,
        xGeneratedContent: xGeneratedContent,
        categories: categoriesIds,
        mediaIds: uploadedImageIds,
        libraryMediaIds: uploadLibraryId,
        taggedUserIds: taggedMemberIds,
        isEditable,  // Feature 4
      };
      const result = await createPost(postData);
      if (result?.status === true) {
        toast.info("Post Created Successfully for Review");
        prepareThePost(result?.postMediaResponseData?.postId);
        setPostId(result?.postMediaResponseData?.postId);
        setPostReview(true);
        // setIsDialogOpen(true);
        setPostData(result);
        // clear();
      } else {
        toast.error(result?.message || "Failed to create post. Please try again.");
        clear();
      }
    } catch (error) {
      console.error("Error in posting:", error.message);
      toast.error(error.message || "Something went wrong while creating the post.");
      clear();
    } finally {
      setLoading(false);
      setLoadingMessage("");
    }
  };
  const [postReviewData, setPostReviewData] = useState(null);

  const prepareThePost = async (id) => {
    try {
      const response = await preparePost(id);
      if (response?.status === true) {
        setPostReviewData(response);
      }
    } catch (error) {
      console.log(error);
    }
  };

  const addCategory = (category) => {
    if (!categories?.some((cat) => cat.name === category.name)) {
      setCategories((prevCategories) => [...prevCategories, category]);
      setCategoriesIds((prevIds) => [...prevIds, category.id]);
      setCategoryInput("");
    }
  };

  const removeCategory = (category) => {
    setCategories(categories?.filter((cat) => cat.name !== category.name));
    setCategoriesIds(categoriesIds?.filter((id) => id !== category.id));
  };

  const addMember = (member) => {
    if (member && !taggedMembers.some((m) => m.id === member.id)) {
      setTaggedMembers((prev) => [...prev, member]);
      setTaggedMemberIds((prev) => [...prev, member.id]);
      setTaggedMemberInput("");
    }
  };

  const removeTaggedMember = (member) => {
    setTaggedMembers((prev) => prev.filter((m) => m.id !== member.id));
    setTaggedMemberIds((prev) => prev.filter((id) => id !== member.id));
  };

  const handleImageUpload = async (e) => {
    console.log(e.target.files);
    const files = Array.from(e.target.files);
    setImages((prevImages) => [...prevImages, ...files]);
    try {
      setLoading(true);
      setLoadingMessage("Uploading...");
      const formData = new FormData();
      files.forEach((file) => {
        formData.append("files", file);
      });
      formData.append("mediaType", "image");
      const response = await uploadMedia(formData);
      if (response?.status === true) {
        setUploadedImageIds((prev) => [
          ...prev,
          ...(response?.mediaUploadData?.imageIds || []),
        ]);
        toast.success("Successfully Added");
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveImage = (image, index) => {
    setImages((prevImages) => prevImages.filter((_, i) => i !== index));
  };

  const handleImageUploader = async (images) => {
    try {
      setOpen(false);
      setLoading(true);
      setLoadingMessage("Uploading...");
      if (images[0] instanceof File) {
        setImages((prev) => [...prev, ...images]);
        const formData = new FormData();
        images.forEach((file) => {
          formData.append("files", file);
        });
        formData.append("mediaType", "image");

        const response = await uploadMedia(formData);
        if (response?.status === true) {
          setUploadedImageIds((prev) => [
            ...prev,
            ...(response?.mediaUploadData?.imageIds || []),
          ]);
          toast.success("Successfully Added");
        }
      } else {
        setImages((prev) => [...prev, ...images]);
        const ids = images.map((img) => img.id);
        // setUploadedImageIds((prev) => [...prev, ...ids]);
        setUploadLibraryId((prev) => [...prev, ...ids]);
        toast.success("Successfully Added from existing list");
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleVideoUpload = async (event) => {
    const uploadedFiles = Array.from(event.target.files);
    const videoPreviews = uploadedFiles?.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
      playing: false,
    }));
    setVideos((prev) => [...prev, ...videoPreviews]);
    try {
      setLoading(true);
      setLoadingMessage("Uploading...");
      const formData = new FormData();
      uploadedFiles.forEach((file) => {
        formData.append("files", file);
      });
      formData.append("mediaType", "video");
      const response = await uploadMedia(formData);
      if (response?.status === true) {
        setUploadedImageIds((prev) => [
          ...prev,
          ...(response?.mediaUploadData?.videoIds || []),
        ]);
        toast.success("Successfully Added");
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleVideoConfirm = async (selectedVideos) => {
    try {
      setLoading(true);
      setLoadingMessage("Uploading...");
      const existingVideos = selectedVideos?.filter((vid) => !vid.isNew);
      const newVideos = selectedVideos?.filter((vid) => vid.isNew);
      let uploadedPreviews = [];
      let uploadedIds = [];
      if (newVideos.length > 0) {
        const formData = new FormData();
        for (const vid of newVideos) {
          formData.append("files", vid.file);
        }
        formData.append("mediaType", "video");
        const response = await uploadMedia(formData);
        if (response?.status === true) {
          uploadedIds = response?.mediaUploadData?.videoIds || [];
          uploadedPreviews = newVideos.map((vid, idx) => ({
            id: uploadedIds[idx],
            preview: vid.preview,
            playing: false,
          }));
        }
      }

      // final combined
      const unified = [
        ...existingVideos.map((vid) => ({
          id: vid.id,
          preview: vid.preview,
          playing: false,
        })),
        ...uploadedPreviews,
      ];
      const existingIds = existingVideos.map((vid) => vid.id);
      setUploadLibraryId((prev) => [...prev, ...existingIds]);
      setUploadedImageIds((prev) => [...prev, ...uploadedIds]);
      // setUploadedImageIds((prev) => [...prev, ...existingIds, ...uploadedIds]);
      setVideos((prev) => [...prev, ...unified]);
      toast.success("Successfully Added");
    } catch (error) {
      console.error("Video upload error:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveVideo = (index) => {
    setVideos((prev) => prev.filter((_, i) => i !== index));
  };

  const handlePauseVideo = (index) => {
    setVideos((prev) =>
      prev.map((video, i) =>
        i === index ? { ...video, playing: false } : video
      )
    );
  };

  const handlePlayVideo = (index) => {
    setVideos((prev) =>
      prev.map((video, i) =>
        i === index ? { ...video, playing: true } : { ...video, playing: false }
      )
    );

    setTimeout(() => {
      const videoEl = document.querySelectorAll("video")[index];
      if (videoEl) videoEl.play();
    }, 0);
  };
  const platforms = ["All", "Linkedin", "Twitter/X", "Instagram", "Facebook"];
  const [selected, setSelected] = useState(["All"]);
  const selectedPublishingPlatforms = selected.filter((platform) => platform !== "All");
  const activePlatformsForLimit =
    selectedPublishingPlatforms.length > 0 ? selectedPublishingPlatforms : ["Linkedin"];

  const getStrictestComposition = (value) =>
    activePlatformsForLimit
      .map((platform) => composePlatformContent(value, platform, businessPages))
      .reduce((smallest, current) =>
        current.maxBaseCharacters < smallest.maxBaseCharacters ? current : smallest
      );

  const handleEditorValueChange = (nextValue) => {
    const clampedValue = activePlatformsForLimit.reduce(
      (currentValue, platform) =>
        clampPlatformContent(currentValue, platform, businessPages),
      nextValue
    );
    setEditorValue(clampedValue);
  };

  const handleChange = (platform) => {
    if (platform === "All") {
      setSelected(["All"]);
    } else {
      console.log(platform, "plat");
      const newSelection = selected.includes(platform)
        ? selected.filter((p) => p !== platform)
        : [...selected.filter((p) => p !== "All"), platform];
      setSelected(newSelection);
    }
  };

  const handleConfirm = () => {
    clear();
    setDiscard(false);
  };

  const handleDiscard = () => {
    clear();
    setDiscard(true);
  };

  const handleClose = () => {
    setPostReview(false);
    setActivities(false);
  };

  const handleActivities = () => {
    setActivities(true);
  };

  const fetchAllUsers = async () => {
    const org_id = sessionStorage.getItem("orgId");
    try {
      const response = await employeeUserListing(org_id);
      if (response?.status === true) {
        setUsersData(response?.users || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };

  const fetchAllTrendingTopics = async () => {
    try {
      const response = await employeeTrendingTopics();
      if (response?.status === true) {
        setTrendsData(response?.trendingTopics || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };

  const [aiLoader, setAiLoader] = useState(false);
  const [showGeneratingPopup, setShowGeneratingPopup] = useState(false);
  const [dots, setDots] = useState(".");
  const [editorValue, setEditorValue] = useState("");
  const [xGeneratedContent, setXGeneratedContent] = useState(null);
  const [apiValue, setApiValue] = useState("");
  const handleTopicInputChange = (value) => {
    setPostText(value);
    if (selectedTrendingTopic && value !== selectedTrendingTopic) {
      setSelectedTrendingTopic("");
    }
  };

  useEffect(() => {
    let interval;
    if (aiLoader) {
      interval = setInterval(() => {
        setDots((prev) => (prev.length >= 2 ? "." : prev + "."));
      }, 1000);
    } else {
      setDots(".");
    }
    return () => clearInterval(interval);
  }, [aiLoader]);
  const aiContentGenerator = async () => {
    try {
      if (!postText) {
        toast.error("Please enter a topic to generate content");
        return;
      }
      setAiLoader(true);
      setShowGeneratingPopup(true);
      const xComposition = composePlatformContent("", "x", businessPages);
      const payload = {
        topic: postText,
        xMaxCharacters: xComposition.maxBaseCharacters || 200,
      };
      const response = await Content_Generation(payload);
      if (response?.contentGenerateResponseDTO?.generatedText) {
        handleEditorValueChange(response.contentGenerateResponseDTO.generatedText);
        setXGeneratedContent(response?.contentGenerateResponseDTO?.xgeneratedContent || null);
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setAiLoader(false);
      setShowGeneratingPopup(false);
    }
  };

  const handleClickTopics = (data) => {
    if (!data) {
      toast.error("Please select a topic");
      return;
    }
    setSelectedTrendingTopic(data);
    setPostText(data);
  };

  useEffect(() => {
    fetchAllTrendingTopics();
  }, []);

  // useEffect(() => {
  //   if (apiValue) {
  //     const htmlWithPoints = convertToNumberedList(apiValue);
  //     setEditorValue(htmlWithPoints);
  //   }
  // }, [apiValue]);

  // const convertToNumberedList = (text) => {
  //   const lines = text
  //     .split(/\n+/) // split on one or more newlines
  //     .map((line) => line.trim())
  //     .filter((line) => line.length > 0);

  //   if (lines.length === 0) return "";

  //   return `<ol>${lines.map((l) => `<li>${l}</li>`).join("")}</ol>`;
  // };

  return (
    <>
      <LoaderOverlay loading={loading} message={loadingMessage} />
      <Card
        className="postCreation-container"
        elevation={0}
        sx={{ padding: 2 }}
      >
        <Box
          display="flex"
          flexDirection={{ xs: "column", sm: "column", md: "row" }}
          // padding="8px 5px 0px 11px"
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", md: "center" }}
          width="100%"
          gap={{ xs: 2, sm: 2, md: 0 }}
        >
          {/* Left Section */}
          <Box
            display="flex"
            width={{ xs: "100%", md: "50%" }}
            alignItems="center"
            // mb={{ xs: 1, md: 2 }}
          >
            {userProfile?.profilePicture ? (
              <Avatar
                sx={{ mr: 2 }}
                src={userProfile?.profilePicture}
                alt={userProfile?.name}
              />
            ) : (
              <Avatar sx={{ mr: 2 }}>{userProfile?.name?.[0]}</Avatar>
            )}
            <Box>
              <Typography
                variant="body1"
                fontWeight={500}
                color="#3B3B3B"
                // fontSize={{ xs: 16, sm: 18, md: 19 }}
              >
                {userProfile?.name || ""}
              </Typography>
              <Typography
                variant="body1"
                color="#95919D"
                fontSize={{ xs: 13, sm: 14, md: 14 }}
              >
                {userProfile?.jobTitle || ""}
              </Typography>
            </Box>
          </Box>

          {/* Right Section */}
          <Box
            display="flex"
            width={{ xs: "100%", md: "50%" }}
            flexDirection={{ xs: "column", sm: "column", md: "row" }}
            borderTop={{ xs: "1px solid #E0E0E0", md: "none" }}
          >
            {/* Points */}
            <Box
              width={{ xs: "100%", md: "54%" }}
              display="flex"
              gap={2}
              justifyContent="center"
              alignItems="center"
              borderRight={{ md: "1px solid #BEBEBE" }}
              borderBottom={{ xs: "1px solid #E0E0E0", md: "none" }}
              py={{ xs: 1, md: 0 }}
            >
              <Typography
                variant="body1"
                fontSize={{ xs: 13, sm: 14, md: 14 }}
                color="#95919D"
              >
                My Total Point:
              </Typography>
              <img src={coins} alt="coin" width={18} height={18} />
              <Typography
                color="#252525"
                fontWeight={600}
                fontSize={{ xs: 18, md: 17 }}
                variant="h6"
              >
                {loyaltyPoints?.loyaltyPoints || 0}
              </Typography>
            </Box>

            {/* Activity */}
            <Box
              alignItems="center"
              justifyContent="end"
              gap={2}
              display="flex"
              width={{ xs: "100%", md: "46%" }}
              onClick={handleActivities}
              sx={{ cursor: "pointer" }}
              py={{ xs: 1, md: 0 }}
            >
              <Typography
                variant="body1"
                fontSize={{ xs: 13, sm: 14, md: 14 }}
                color="#95919D"
              >
                My Activity:
              </Typography>
              <img src={message} alt="post" width={18} height={18} />
              <Typography
                color="#252525"
                fontWeight={600}
                fontSize={{ xs: 18, md: 17 }}
                variant="h6"
              >
                {/* 4633 */}
              </Typography>
            </Box>
          </Box>
        </Box>

        <Grid
          container
          // padding={"0 16px 0 16px"}
          sx={{
            minHeight: "100%",
            height: "100%",
            display: "flex",
            marginLeft: "0px !important",
          }}
          spacing={2}
          alignItems="stretch"
        >
          <Grid
            pl={0}
            sx={{
              display: "flex",
              flexDirection: "column",
              paddingLeft: "0px !important",
              paddingTop: "45px !important",
            }}
            // mt={0.5}
            item
            xs={12}
            md={6}
            sm={12}
          >
            <Box
              sx={{
                display: "flex",
                backgroundColor: "#F8F9FC",
                border: "1px solid #e0e0e0",
                borderRadius: "12px",
                padding: "12px",
                flexDirection: "row",
                flexWrap: "wrap",
                alignItems: "flex-start",
              }}
            >
              <TextField
                variant="standard"
                placeholder="Compose"
                InputProps={{
                  disableUnderline: true,
                  sx: {
                    fontSize: "14px",
                    flex: 1,
                    color: "#7c7676",
                    backgroundColor: "transparent",
                  },
                }}
                multiline
                fullWidth
                value={postText}
                onChange={(e) => handleTopicInputChange(e.target.value)}
                sx={{
                  flex: 1,
                  minWidth: "200px",
                  ".MuiInputBase-root": {
                    backgroundColor: "transparent",
                  },
                }}
              />
              {aiLoader ? (
                <Button
                  variant="contained"
                  sx={{
                    width: "132px",
                    background: "linear-gradient(to right, #BC77F4, #0047AB)",
                    color: "#fff",
                    borderRadius: "8px",
                    textTransform: "none",
                    fontSize: "13px",
                    padding: "5px 10px",
                    boxShadow: "none",
                    fontWeight: 500,
                    alignSelf: "flex-end",

                    "&:hover": {
                      background: "linear-gradient(to right, #8a6be9, #306ddf)",
                      boxShadow: "none",
                    },
                  }}
                >
                  ✨ Generating{dots}
                </Button>
              ) : (
                <Button
                  variant="contained"
                  sx={{
                    width: "132px",
                    background: "linear-gradient(to right, #BC77F4, #0047AB)",
                    color: "#fff",
                    borderRadius: "8px",
                    fontSize: "13px",
                    textTransform: "none",
                    padding: "5px 10px",
                    boxShadow: "none",
                    fontWeight: 500,
                    alignSelf: "flex-end",

                    "&:hover": {
                      background: "linear-gradient(to right, #8a6be9, #306ddf)",
                      boxShadow: "none",
                    },
                  }}
                  onClick={aiContentGenerator}
                  disabled={aiLoader}
                >
                  {aiLoader ? "Generating..." : "✨ Generate"}
                </Button>
              )}
            </Box>

            <Box
              display="flex"
              // mt={1}
              flexWrap="wrap"
              alignItems="center"
              mb={2}
            >
              <Autocomplete
                freeSolo
                options={categoriesData.filter((category) =>
                  category?.name
                    ?.toLowerCase()
                    ?.includes(categoryInput.toLowerCase())
                )}
                value={categoryInput}
                onInputChange={(event, newInputValue) =>
                  setCategoryInput(newInputValue)
                }
                onChange={(event, newValue) => addCategory(newValue)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    variant="standard"
                    sx={{
                      color: "#E0E0E0",
                      "& .MuiInputBase-input": {
                        fontSize: "12px",
                      },
                    }}
                    placeholder="Enter Categories"
                    className="categoriesField"
                  />
                )}
                PopperComponent={(props) => (
                  <Popper
                    {...props}
                    sx={{
                      padding: "8px",
                      "& .MuiAutocomplete-listbox": {
                        padding: "5px",
                        border: "1px solid #DEDFE5",
                        borderRadius: "8px",
                        "& li": {
                          backgroundColor: "#FFFFFF",
                          padding: "8px",
                          borderRadius: "8px",
                          cursor: "pointer",
                          transition: "background-color 0.3s ease",
                        },
                        "& li:hover": {
                          backgroundColor: "#0047AB",
                          color: "#FFFFFF",
                        },
                      },
                    }}
                  />
                )}
                // getOptionLabel={(option) => option}
                getOptionLabel={(option) =>
                  typeof option === "string" ? option : option?.name || ""
                }
                disableClearable
              />
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                {categories?.map((category, index) => (
                  <Chip
                    className="taggedMembers-cards"
                    key={index}
                    label={category?.name}
                    onDelete={() => removeCategory(category)}
                    deleteIcon={<CloseIcon />}
                  />
                ))}
              </Box>
            </Box>

            {/* Feature 4: Editable / Non-editable toggle */}
            <Box sx={{ px: 3, py: 1.5, display: "flex", alignItems: "center", gap: 2, bgcolor: isEditable ? "#f0f7ff" : "#fff4f4", borderRadius: 2, mx: 2, mb: 1 }}>
              <Switch checked={isEditable} onChange={(e) => setIsEditable(e.target.checked)} color="primary" />
              <Box>
                <Typography variant="body2" fontWeight={600}>{isEditable ? "✏️ Editable" : "🔒 Locked (Quick-Share Only)"}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {isEditable ? "Employees can edit the text before sharing." : "Legal-sensitive post — employees share as-is without editing."}
                </Typography>
              </Box>
            </Box>

            <Box
              sx={{
                backgroundColor: "#f8f6fe",
                padding: "16px 24px",
                pt: 0,
                //   borderRadius: "8px",
                overflowX: "auto",
                whiteSpace: "nowrap",
              }}
            >
              <Box
                display="flex"
                flexDirection="column"
                mt={1}
                gap={1}
                width="50%"
              >
                {/* <Autocomplete
                  freeSolo
                  options={trendsData.filter(
                    (trend) =>
                      trend.toLowerCase().includes(trendsInput.toLowerCase()) &&
                      !selectedTrends.includes(trend)
                  )}
                  inputValue={trendsInput}
                  onInputChange={(event, newInputValue) =>
                    setTrendsInput(newInputValue)
                  }
                  onChange={(event, newValue) => addTrends(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      variant="standard"
                      sx={{
                        color: "#E0E0E0",
                        "& .MuiInputBase-input": {
                          fontSize: "12px",
                        },
                      }}
                      placeholder="Trending Topics"
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          addTrends(trendsInput);
                        }
                      }}
                    />
                  )}
                  PopperComponent={(props) => (
                    <Popper
                      {...props}
                      sx={{
                        width: "200px",
                        padding: "8px",
                        "& .MuiAutocomplete-listbox": {
                          padding: "5px",
                          border: "1px solid #DEDFE5",
                          borderRadius: "8px",
                          "& li": {
                            backgroundColor: "#FFFFFF",
                            padding: "8px",
                            borderRadius: "8px",
                            cursor: "pointer",
                            transition: "background-color 0.3s ease",
                          },
                          "& li:hover": {
                            backgroundColor: "#0047AB",
                            color: "#FFFFFF",
                          },
                        },
                      }}
                    />
                  )}
                  getOptionLabel={(option) =>
                    typeof option === "string" ? option : option || ""
                  }
                  disableClearable
                /> */}
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                  <Typography
                    variant="body2"
                    sx={{ color: "#95919D", fontWeight: 400, mb: 0.5 }}
                  >
                    Trending Topics
                  </Typography>
                  <Box
                    sx={{
                      display: "flex",
                      gap: 2,
                      flexWrap: "nowrap",
                    }}
                  >
                    {trendsData?.map((topic, index) => (
                      <Chip
                        onClick={() => handleClickTopics(topic)}
                        key={index}
                        label={topic}
                        variant="outlined"
                        sx={{
                          backgroundColor:
                            selectedTrendingTopic === topic ? "#d9e7ff" : "#eef2fb",
                          borderColor:
                            selectedTrendingTopic === topic ? "#0047AB" : "#73a3f3",
                          borderRadius: "24px",
                          fontSize: "11px",
                          fontWeight: selectedTrendingTopic === topic ? 500 : "400",
                          padding: "3px 8px",
                          color: selectedTrendingTopic === topic ? "#0047AB" : "#222",
                          height: "auto",
                          whiteSpace: "nowrap",
                        }}
                      />
                    ))}
                  </Box>
                  {/* {selectedTrends?.map((trend, index) => (
                    <Chip
                      key={index}
                      label={trend}
                      onDelete={() => removeTrends(trend)}
                      deleteIcon={<CloseIcon fontSize="10px" />}
                      sx={{
                        backgroundColor: "#E7F1FF",
                        border: "1px solid #2D76DC",
                        color: "#2D76DC",
                        fontWeight: 400,
                        fontSize: "10px",
                        height: "24px",
                      }}
                    />
                  ))} */}
                </Box>
              </Box>
            </Box>

            {images?.length > 0 && (
              <Box mt={2} className="img_container">
                <Typography color="#9C9C9C">
                  {`Upload from Library : ${images?.length}`}
                </Typography>
                <Box mt={1} width={"100%"} className="image-container">
                  <IconButton component="label" className="AddIcon">
                    <AddIcon />
                    <input
                      type="file"
                      accept="image/*"
                      multiple
                      onChange={handleImageUpload}
                      hidden
                    />
                  </IconButton>

                  {/* SCROLLABLE IMAGES ONLY */}
                  <Box ml={2} className="images-scroll-area">
                    {images?.map((image, index) => (
                      <Box className="imageBox" key={index}>
                        <img
                          src={
                            image instanceof File
                              ? URL.createObjectURL(image) // System se upload
                              : image.url // Already uploaded list
                          }
                          alt={`uploaded-${index}`}
                          className="containerImage"
                        />
                        <IconButton
                          size="small"
                          onClick={() => handleRemoveImage(image, index)}
                          className="deleteImage"
                        >
                          <svg
                            width="12"
                            height="12"
                            viewBox="0 0 18 18"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                          >
                            <path
                              d="M15.3751 4.5H2.625"
                              stroke="white"
                              strokeLinecap="round"
                            />
                            <path
                              d="M14.125 6.375L13.78 11.5493C13.6473 13.5405 13.5809 14.5361 12.9322 15.1431C12.2834 15.75 11.2856 15.75 9.29001 15.75H8.70999C6.71439 15.75 5.71659 15.75 5.06783 15.1431C4.41907 14.5361 4.3527 13.5405 4.21996 11.5493L3.875 6.375"
                              stroke="white"
                              strokeLinecap="round"
                            />
                            <path
                              d="M7.125 8.25L7.5 12"
                              stroke="white"
                              strokeLinecap="round"
                            />
                            <path
                              d="M10.875 8.25L10.5 12"
                              stroke="white"
                              strokeLinecap="round"
                            />
                            <path
                              d="M4.875 4.5C4.91691 4.5 4.93786 4.5 4.95686 4.49952C5.57444 4.48387 6.11927 4.09118 6.32941 3.51024C6.33588 3.49237 6.3425 3.47249 6.35576 3.43273L6.42857 3.21429C6.49073 3.02781 6.52181 2.93457 6.56304 2.8554C6.72751 2.53955 7.03181 2.32023 7.38346 2.26407C7.4716 2.25 7.56988 2.25 7.76645 2.25H10.2336C10.4301 2.25 10.5284 2.25 10.6165 2.26407C10.9682 2.32023 11.2725 2.53955 11.437 2.8554C11.4782 2.93457 11.5093 3.02781 11.5714 3.21429L11.6442 3.43273C11.6575 3.47244 11.6641 3.49238 11.6706 3.51024C11.8807 4.09118 12.4256 4.48387 13.0431 4.49952C13.0621 4.5 13.0831 4.5 13.125 4.5"
                              stroke="white"
                            />
                          </svg>
                          Delete
                        </IconButton>
                      </Box>
                    ))}
                  </Box>
                </Box>
              </Box>
            )}

            {videos.length > 0 && (
              <Box
                display="flex"
                gap={2}
                sx={{ maxHeight: 400, overflowY: "auto" }}
                flexWrap="wrap"
                mt={2}
                mb={2}
              >
                {videos.map((video, index) => (
                  <Box key={index} className="videoContainer">
                    <video
                      src={video.preview}
                      className="videos"
                      controls={videos[index].playing}
                      // onClick={(e) => e.target.pause()}
                      // onPause={() => handlePlayVideo(index)}
                      onPlay={() => handlePlayVideo(index)}
                      onPause={() => handlePauseVideo(index)}
                    />
                    {!video.playing && (
                      <Box
                        className="playButton"
                        onClick={() => handlePlayVideo(index)}
                      >
                        <Box className="playIcon">
                          <PlayArrowIcon
                            style={{ fontSize: 35, color: "#0047AB" }}
                          />
                        </Box>
                      </Box>
                    )}
                    <IconButton
                      size="small"
                      style={{
                        position: "absolute",
                        top: 5,
                        background: "#FFFFFF",
                        right: 5,
                        color: "#fff",
                      }}
                      onClick={() => handleRemoveVideo(index)}
                    >
                      <CloseIcon
                        className="closeIcon"
                        sx={{ color: "#0047AB" }}
                      />
                    </IconButton>
                  </Box>
                ))}
              </Box>
            )}

            {isTaggingVisible && (
              <Box className="tagging-container" p={1}>
                <Typography className="count">{`Members Tagged : ${taggedMembers?.length}`}</Typography>

                <Autocomplete
                  freeSolo={false} // ✅ keep only userData objects, not free text
                  options={userData || []}
                  getOptionLabel={(option) => option?.name || ""} // ✅ show name
                  value={null} // avoid controlled warning, we only use input value
                  inputValue={taggedMemberInput}
                  onInputChange={(event, newInputValue) =>
                    setTaggedMemberInput(newInputValue)
                  }
                  onChange={(event, newValue) => {
                    if (newValue) addMember(newValue);
                  }}
                  renderOption={(props, option) => (
                    <Box
                      component="li"
                      {...props}
                      key={option.id}
                      sx={{ display: "flex", alignItems: "center", gap: 1 }}
                    >
                      <Avatar
                        src={option.profilePictureUrl || ""}
                        alt={option.name}
                        sx={{ width: 28, height: 28 }}
                      >
                        {option.name?.charAt(0)}
                      </Avatar>
                      <Box>
                        <Typography fontSize="14px" fontWeight={500}>
                          {option.name}
                        </Typography>
                        <Typography fontSize="12px" color="text.secondary">
                          {option.email}
                        </Typography>
                      </Box>
                    </Box>
                  )}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder="Search Team Members"
                      size="small"
                      variant="outlined"
                      className="customInput"
                      InputProps={{
                        ...params.InputProps,
                        startAdornment: (
                          <>
                            {params.InputProps.startAdornment}
                            <InputAdornment position="start">
                              <SearchIcon
                                style={{ color: "#2D76DC", fontSize: "20px" }}
                              />
                            </InputAdornment>
                          </>
                        ),
                      }}
                    />
                  )}
                  PopperComponent={(props) => (
                    <Popper
                      {...props}
                      sx={{
                        padding: "8px",
                        "& .MuiAutocomplete-listbox": {
                          padding: "5px",
                          border: "1px solid #DEDFE5",
                          borderRadius: "8px",
                          "& li": {
                            backgroundColor: "#FFFFFF",
                            padding: "8px",
                            borderRadius: "8px",
                            cursor: "pointer",
                            transition: "background-color 0.3s ease",
                          },
                          "& li:hover": {
                            backgroundColor: "#0047AB",
                            color: "#FFFFFF",
                          },
                        },
                      }}
                    />
                  )}
                  disableClearable
                />

                {/* Tagged Members List */}
                <Box
                  sx={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: 1,
                    marginTop: "15px",
                  }}
                >
                  {taggedMembers.map((member) => (
                    <Chip
                      key={member.id}
                      avatar={
                        <Avatar src={member.profilePictureUrl || ""}>
                          {member.name?.charAt(0)}
                        </Avatar>
                      }
                      label={member.name}
                      onDelete={() => removeTaggedMember(member)}
                      deleteIcon={<CloseIcon />}
                      className="taggedMembers-cards"
                    />
                  ))}
                </Box>
              </Box>
            )}
          </Grid>

          <Grid
            pl={0}
            //  sx={{ height: "100%" }}
            sx={{ display: "flex", flexDirection: "column" }}
            item
            xs={12}
            md={6}
            sm={12}
          >
            <Typography variant="body2" sx={{ mb: 1 }} color="#95919D">
              Result Generated
            </Typography>
            <Box sx={{ flex: 1 }}>
              <EditorBox value={editorValue} onChange={handleEditorValueChange} />
            </Box>
            <Typography
              variant="body2"
              sx={{ mt: 1, textAlign: "end" }}
              color="#95919D"
            >
              {(() => {
                const composition = getStrictestComposition(editorValue);
                return `Remaining: ${composition.remainingCharacters} / ${composition.maxBaseCharacters} (${composition.platformKey.toUpperCase()} limit after suffix)`;
              })()}
            </Typography>
          </Grid>
        </Grid>

        {/* Categories Input */}

        <Box className="buttons-container">
          {/* Action Buttons */}
          <Box className="actionButtons">
            <Box>
              <IconButton
                color="primary"
                sx={{ padding: "8px 8px 8px 0px !important" }}
                onClick={() => {
                  setOpen(true);
                  setActiveButton("image");
                }}
              >
                <svg
                  width="32"
                  height="32"
                  viewBox="0 0 44 44"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className={activeButton === "image" ? "tagClicked" : undefined}
                >
                  <rect width="44" height="44" rx="8" fill="#F4F0FF" />
                  <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M27.0422 20.582C25.0977 20.582 24.1254 20.582 23.5213 19.9779C22.9172 19.3738 22.9172 18.4016 22.9172 16.457C22.9172 14.5125 22.9172 13.5402 23.5213 12.9361C24.1254 12.332 25.0977 12.332 27.0422 12.332C28.9868 12.332 29.9591 12.332 30.5631 12.9361C31.1672 13.5402 31.1672 14.5125 31.1672 16.457C31.1672 18.4016 31.1672 19.3738 30.5631 19.9779C29.9591 20.582 28.9868 20.582 27.0422 20.582ZM27.7297 14.6237C27.7297 14.244 27.4219 13.9362 27.0422 13.9362C26.6625 13.9362 26.3547 14.244 26.3547 14.6237V15.7695H25.2089C24.8292 15.7695 24.5214 16.0773 24.5214 16.457C24.5214 16.8367 24.8292 17.1445 25.2089 17.1445H26.3547V18.2904C26.3547 18.6701 26.6625 18.9779 27.0422 18.9779C27.4219 18.9779 27.7297 18.6701 27.7297 18.2904V17.1445H28.8756C29.2553 17.1445 29.5631 16.8367 29.5631 16.457C29.5631 16.0773 29.2553 15.7695 28.8756 15.7695H27.7297V14.6237Z"
                    fill="#0047AB"
                  />
                  <path
                    d="M31.1675 22.1384C31.1661 23.4855 31.1558 24.6305 31.0793 25.57C30.9905 26.6602 30.8084 27.5712 30.401 28.3278C30.2213 28.6616 30.0005 28.9604 29.7316 29.2293C28.9686 29.9923 27.9965 30.3379 26.7645 30.5036C25.5609 30.6654 24.0183 30.6654 22.0499 30.6654H21.9521C19.9837 30.6654 18.4411 30.6654 17.2375 30.5036C16.0056 30.3379 15.0335 29.9923 14.2704 29.2293C13.594 28.5528 13.2447 27.7109 13.0589 26.6665C12.8764 25.6405 12.8431 24.3641 12.8361 22.779C12.8344 22.3759 12.8344 21.9494 12.8344 21.4996V21.4498C12.8343 19.4814 12.8343 17.9388 12.9962 16.7352C13.1618 15.5032 13.5074 14.5311 14.2704 13.7681C15.0335 13.0051 16.0056 12.6595 17.2375 12.4938C18.3079 12.3499 19.685 12.334 21.3612 12.3322C21.7144 12.3319 22.001 12.6184 22.001 12.9716C22.001 13.3248 21.7143 13.611 21.3611 13.6113C19.6612 13.6131 18.3962 13.6286 17.4079 13.7615C16.3178 13.9081 15.6606 14.1868 15.1749 14.6726C14.6891 15.1583 14.4104 15.8155 14.2638 16.9056C14.1148 18.0141 14.1134 19.4708 14.1134 21.4987C14.1134 21.7464 14.1134 21.9859 14.1137 22.2178L14.9674 21.4708C15.7444 20.7909 16.9156 20.8299 17.6457 21.56L21.3036 25.2179C21.8896 25.8039 22.8121 25.8838 23.4901 25.4073L23.7444 25.2286C24.7201 24.5429 26.0402 24.6223 26.9266 25.4201L29.3402 27.5923C29.5831 27.0821 29.7274 26.4118 29.8044 25.4661C29.877 24.5751 29.887 23.4929 29.8884 22.1384C29.8888 21.7852 30.1749 21.4987 30.5282 21.4987C30.8814 21.4987 31.1678 21.7852 31.1675 22.1384Z"
                    fill="#ABC8F0"
                  />
                </svg>
              </IconButton>
              <input
                type="file"
                accept="image/*"
                multiple
                hidden
                id="modal-device-upload"
                onChange={(e) => {
                  const files = Array.from(e.target.files);
                  handleImageUploader(files);
                }}
                style={{ display: "none" }}
              />
              <IconButton
                color="primary"
                sx={{ padding: "8px 8px 8px 0px !important" }}
                onClick={() => {
                  setOpenVideoModal(true);
                  setActiveButton("video");
                }}
              >
                <svg
                  width="32"
                  height="32"
                  viewBox="0 0 44 44"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className={activeButton === "video" ? "tagClicked" : undefined}
                >
                  <rect width="44" height="44" rx="8" fill="#F4F0FF" />
                  <path
                    d="M12.8333 20.541C12.8333 17.5275 12.8333 16.0207 13.6655 15.0066C13.8179 14.8209 13.9882 14.6507 14.1738 14.4983C15.188 13.666 16.6947 13.666 19.7083 13.666C22.7218 13.666 24.2285 13.666 25.2427 14.4983C25.4284 14.6507 25.5986 14.8209 25.751 15.0066C26.5833 16.0207 26.5833 17.5275 26.5833 20.541V21.4577C26.5833 24.4712 26.5833 25.978 25.751 26.9921C25.5986 27.1778 25.4284 27.348 25.2427 27.5004C24.2285 28.3327 22.7218 28.3327 19.7083 28.3327C16.6947 28.3327 15.188 28.3327 14.1738 27.5004C13.9882 27.348 13.8179 27.1778 13.6655 26.9921C12.8333 25.978 12.8333 24.4712 12.8333 21.4577V20.541Z"
                    fill="#ABC8F0"
                  />
                  <path
                    d="M26.5833 18.7082L27.1867 18.4064C28.9705 17.5146 29.8623 17.0686 30.5145 17.4717C31.1666 17.8747 31.1666 18.8718 31.1666 20.8661V21.1336C31.1666 23.1279 31.1666 24.125 30.5145 24.528C29.8623 24.9311 28.9705 24.4851 27.1867 23.5933L26.5833 23.2915V18.7082Z"
                    fill="#0047AB"
                  />
                  <path
                    d="M20.3959 18.709C20.3959 18.3293 20.0881 18.0215 19.7084 18.0215C19.3287 18.0215 19.0209 18.3293 19.0209 18.709V20.3132H17.4167C17.0371 20.3132 16.7292 20.621 16.7292 21.0007C16.7292 21.3803 17.0371 21.6882 17.4167 21.6882H19.0209V23.2923C19.0209 23.672 19.3287 23.9798 19.7084 23.9798C20.0881 23.9798 20.3959 23.672 20.3959 23.2923V21.6882H22.0001C22.3798 21.6882 22.6876 21.3803 22.6876 21.0007C22.6876 20.621 22.3798 20.3132 22.0001 20.3132H20.3959V18.709Z"
                    fill="#0047AB"
                  />
                </svg>
              </IconButton>
              <input
                id="video-upload-input"
                type="file"
                accept="video/*"
                multiple
                onChange={handleVideoUpload}
                style={{ display: "none" }}
              />
              <IconButton
                sx={{ padding: "8px 8px 8px 0px !important" }}
                color="primary"
                onClick={() => {
                  setIsTaggingVisible(!isTaggingVisible);
                  fetchAllUsers();
                  setActiveButton("tagging");
                }}
              >
                <svg
                  width="32"
                  height="32"
                  viewBox="0 0 44 44"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  className={activeButton === "tagging" ? "tagClicked" : undefined}
                >
                  <rect width="44" height="44" rx="8" fill="#F4F0FF" />
                  <circle
                    cx="21.9999"
                    cy="15.5007"
                    r="3.66667"
                    fill="#0047AB"
                  />
                  <path
                    d="M27.5868 23.7779C27.1974 23.7493 26.7196 23.7493 26.1249 23.7493C24.6125 23.7493 23.8563 23.7493 23.3864 24.2192C22.9166 24.689 22.9166 25.4453 22.9166 26.9577C22.9166 28.0268 22.9166 28.718 23.0826 29.1974C22.7306 29.2316 22.3689 29.2493 21.9999 29.2493C18.4561 29.2493 15.5833 27.6077 15.5833 25.5827C15.5833 23.5576 18.4561 21.916 21.9999 21.916C24.3956 21.916 26.4846 22.6662 27.5868 23.7779Z"
                    fill="#ABC8F0"
                  />
                  <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M26.1251 30.1667C24.6127 30.1667 23.8564 30.1667 23.3866 29.6968C22.9167 29.227 22.9167 28.4708 22.9167 26.9583C22.9167 25.4459 22.9167 24.6897 23.3866 24.2198C23.8564 23.75 24.6127 23.75 26.1251 23.75C27.6375 23.75 28.3937 23.75 28.8636 24.2198C29.3334 24.6897 29.3334 25.4459 29.3334 26.9583C29.3334 28.4708 29.3334 29.227 28.8636 29.6968C28.3937 30.1667 27.6375 30.1667 26.1251 30.1667ZM26.6598 25.5324C26.6598 25.2371 26.4204 24.9977 26.1251 24.9977C25.8298 24.9977 25.5904 25.2371 25.5904 25.5324V26.4236H24.6992C24.4038 26.4236 24.1644 26.663 24.1644 26.9583C24.1644 27.2537 24.4038 27.4931 24.6992 27.4931H25.5904V28.3843C25.5904 28.6796 25.8298 28.919 26.1251 28.919C26.4204 28.919 26.6598 28.6796 26.6598 28.3843V27.4931H27.551C27.8463 27.4931 28.0857 27.2537 28.0857 26.9583C28.0857 26.663 27.8463 26.4236 27.551 26.4236H26.6598V25.5324Z"
                    fill="#0047AB"
                  />
                </svg>
              </IconButton>
            </Box>
            <Box
              ml={1}
              flexWrap="wrap"
              sx={{
                flexDirection: {
                  xs: "column", // Mobile (0px - 600px) -> vertical
                  sm: "row", // Tablet and above -> horizontal
                },
                alignItems: {
                  xs: "flex-start",
                  sm: "center",
                },
              }}
              display="flex"
              alignItems="center"
              gap={1.2}
            >
              <Typography sx={{ color: "#9c9ba3", fontWeight: 500 }}>
                Post To:
              </Typography>
              {platforms?.map((platform) => (
                <FormControlLabel
                  key={platform}
                  label={platform}
                  control={
                    <Checkbox
                      checked={selected.includes(platform)}
                      onChange={() => handleChange(platform)}
                      sx={{
                        color: "#c2c2c2",
                        "&.Mui-checked": {
                          color: "#0047ab",
                        },
                        "& .MuiSvgIcon-root": {
                          width: "20px",
                          height: "20px",
                        },
                        borderRadius: "4px",
                        padding: "9px 0px 9px 5px",
                      }}
                    />
                  }
                  sx={{
                    marginRight: 1,
                    ".MuiTypography-root": {
                      fontSize: "13px !important",
                      fontWeight: 400,
                      color: "#3B3B3B",
                    },
                  }}
                />
              ))}
            </Box>
          </Box>

          {/* Footer Buttons */}
          <Box className="footer-buttons">
            <Button onClick={() => handleDiscard()} className="cancelButton" disabled={loading}>
              Cancel
            </Button>
            <Button
              className={
                editorValue.trim() ? "postButton" : "postButtonDisabled"
              }
              onClick={handlePost}
              disabled={!editorValue.trim() || loading}
            >
              Post
            </Button>
          </Box>
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
            Discard Post?
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
              You have unsaved changes. Are you sure you want to cancel and
              discard this post?
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
              onClick={() => setDiscard(false)}
              sx={{
                backgroundColor: "#F4F0FF",
                color: "#0047AB",
                borderRadius: "8px",
                textTransform: "none",
                border: "1px solid #F4F0FF",
                // px: 4,
                // py: 1,
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
              variant="contained"
              sx={{
                backgroundColor: "#0047AB",
                color: "#fff",
                borderRadius: "8px",
                textTransform: "none",
                // px: 4,
                // py: 1,
                fontWeight: 500,
                fontSize: "16px",
                "&:hover": {
                  backgroundColor: "#002f8c",
                },
              }}
            >
              Confirm
            </Button>
          </DialogActions>
        </Dialog>

        {open && (
          <ImageLibraryModal
            open={open}
            onClose={() => setOpen(false)}
            onConfirm={handleImageUploader}
          />
        )}
        {openVideoModal && (
          <VideoLibraryModal
            open={openVideoModal}
            onClose={() => setOpenVideoModal(false)}
            onConfirm={handleVideoConfirm}
          />
        )}
        {postReview && postReviewData && (
          <SharePostPopup
            open={postReview}
            title="Post Preview"
            type="post"
            onClose={handleClose}
            postReviewData={postReviewData}
            clear={clear}
            selected={selected}
            summary={""}
          />
        )}
        {/* {activities && ( */}
        <ActivitiesPopup open={activities} onClose={handleClose} />
        {/* )} */}
        <StatusPopup
          open={showGeneratingPopup}
          onClose={() => setShowGeneratingPopup(false)}
        />
      </Card>
    </>
  );
};

export default TextEditor;
