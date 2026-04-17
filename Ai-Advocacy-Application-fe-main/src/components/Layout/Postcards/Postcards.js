import React, { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  CardHeader,
  Avatar,
  CardMedia,
  Box,
  IconButton,
  Stack,
  Menu,
  MenuItem,
  Button,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  Switch,
  DialogActions,
  Checkbox,
  List,
  ListItem,
  ListItemText,
  Tooltip,
  CircularProgress,
  Chip,
} from "@mui/material";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import "./Postcard.css";
import PostDailog from "../PostDailog/Postdailog";
import edit from "../../../assets/edit.png";
import share from "../../../assets/share.png";
import CloseIcon from "@mui/icons-material/Close";
import FacebookIcon from "../../../assets/facebook.png";
import InstagramIcon from "../../../assets/insta.png";
import LinkedInIcon from "../../../assets/linkedin.png";
import TwitterIcon from "../../../assets/twitter.png";
import noImage from "../../../assets/noimage.jpg";
import SharePostPopup from "../../sharePostPopup/SharePostPopup";
import PostDetails from "./PostDetails";
import SellIcon from "@mui/icons-material/Sell";
import {
  assignCategoryToPost,
  createFacebookExternalPost,
  createInstagramExternalPost,
  createLinkedinExternalPost,
  createXExternalPost,
  getPosts,
  postDetailsByPostId,
  postSummary,
  preparePost,
} from "../../../services/postService";
import { useLocation, Link } from "react-router-dom";
import { Auth } from "../../../contexts/AuthContext";
import { toast } from "react-toastify";
import { composePlatformContent } from "../../../shared/businessPageComposer";
import {
  getBunnyEmbedUrl,
  getResolvedImageUrl,
  getResolvedPosterUrl,
  getResolvedVideoUrl,
  isBunnyStreamMedia,
} from "../../../shared/mediaResolver";

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

const platform = [
  { name: "Facebook", icon: FacebookIcon },
  { name: "Instagram", icon: InstagramIcon },
  { name: "Linkedin", icon: LinkedInIcon },
  { name: "Twitter / X", icon: TwitterIcon },
];

const PLATFORM_ICON_MAP = {
  FACEBOOK: FacebookIcon,
  INSTAGRAM: InstagramIcon,
  LINKEDIN: LinkedInIcon,
  X: TwitterIcon,
  TWITTER: TwitterIcon,
  TWEET: TwitterIcon,
};

const PLATFORM_LABEL_MAP = {
  FACEBOOK: "Facebook",
  INSTAGRAM: "Instagram",
  LINKEDIN: "LinkedIn",
  X: "X",
  TWITTER: "X",
  TWEET: "X",
};

const getPlatformIcon = (post) => PLATFORM_ICON_MAP[post?.type?.toUpperCase()] || null;
const getPlatformLabel = (post) => PLATFORM_LABEL_MAP[post?.type?.toUpperCase()] || post?.type;

const getDisplayName = (post) => post?.sourceName || post?.createdBy?.name;
const getDisplayUsername = (post) => post?.sourceUsername || null;
const getDisplayAvatar = (post) => {
  if (post?.sourceAvatarUrl) return post.sourceAvatarUrl;
  // Only fall back to user avatar for manual posts (user-created content)
  if (!post?.sourceType || post?.sourceType === "MANUAL") return post?.createdBy?.profileImageUrl;
  return null; // Business page / leader posts show default avatar when no platform avatar
};

const getSourceLabel = (post) => {
  if (post?.sourceType === "LEADER_PROFILE") {
    return "Leader Profile";
  }
  if (post?.sourceType === "BUSINESS_PAGE") {
    return "Business Page";
  }
  return null;
};

const PostCards = () => {
  const location = useLocation();
  const loca = location.pathname.split("/").filter(Boolean).pop();
  const queryParams = new URLSearchParams(location.search);
  const id = queryParams.get("category");
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [selectedPost, setSelectedPost] = useState(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [postsList, setPostsList] = useState([]);
  const [postDetails, setPostDetails] = useState(false);
  const [singlePost, setSinglePost] = useState(null);
  const [clickedPost, setClickedPost] = useState([]);
  const [selected, setSelected] = useState("All");
  const [openSharePopup, setOpenSharePopup] = useState(false);
  const [postReviewData, setPostReviewData] = useState(null);
  const [currentImageIndexes, setCurrentImageIndexes] = useState({});
  const [openDialog, setOpenDialog] = useState(false);
  const [showTagMap, setShowTagMap] = useState({});
  // Feature 1: Dynamic category tabs (replaces platform tabs)
  const [categoryTabs, setCategoryTabs] = useState([]);
  // Feature 7/8/9: Featured posts
  const [featuredPosts, setFeaturedPosts] = useState([]);
  const [featureDialogOpen, setFeatureDialogOpen] = useState(false);
  const [featureTarget, setFeatureTarget] = useState(null);
  const [featuredUntilDays, setFeaturedUntilDays] = useState(7);
  const [showTag, setShowTag] = useState(false);
  const [openCategoriesDialog, setOpenCategoriesDialog] = useState(false);
  const [shareLoading, setShareLoading] = useState(false);
  const authContext = Auth();
  const { businessPages } = authContext;
  const isAdmin = JSON.parse(sessionStorage.getItem("role") || "[]")
    .flat().includes("ROLE_ADMIN");
  const platformKeyMap = {
    Facebook: "facebook",
    Instagram: "instagram",
    Linkedin: "linkedin",
    X: "x",
  };
  const categoryList = authContext;
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(20);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [loadingPostId, setLoadingPostId] = useState(null);
  const [summary, setSummary] = useState("");
  const [activeBunnyPlayers, setActiveBunnyPlayers] = useState({});
  const handleMenuOpen = (event, postIndex) => {
    setMenuAnchor(event.currentTarget);
    setSelectedPost(postIndex);
  };
  const handleMenuClose = () => {
    setMenuAnchor(null);
    setSelectedPost(null);
  };

  const handleCardClick = (post) => {
    setClickedPost(post);
  };
  const handleNext = (postId, imagesLength) => {
    setCurrentImageIndexes((prev) => ({
      ...prev,
      [postId]:
        (prev[postId] ?? 0) === imagesLength - 1 ? 0 : (prev[postId] ?? 0) + 1,
    }));
    setActiveBunnyPlayers((prev) => {
      const next = { ...prev };
      delete next[postId];
      return next;
    });
  };

  const handlePrev = (postId, imagesLength) => {
    setCurrentImageIndexes((prev) => ({
      ...prev,
      [postId]:
        (prev[postId] ?? 0) === 0 ? imagesLength - 1 : (prev[postId] ?? 0) - 1,
    }));
    setActiveBunnyPlayers((prev) => {
      const next = { ...prev };
      delete next[postId];
      return next;
    });
  };

  const handleActivateBunnyPlayer = (postId, mediaIndex, e) => {
    e.stopPropagation();
    setActiveBunnyPlayers((prev) => ({
      ...prev,
      [postId]: mediaIndex,
    }));
  };

  const [checked, setChecked] = useState({
    Facebook: false,
    Instagram: false,
    Linkedin: false,
    "Twitter / X": false,
  });

  const handleToggle = (platform) => {
    setChecked((prev) => ({
      ...prev,
      [platform]: !prev[platform],
    }));
  };

  const onClose = () => {
    setOpenDialog(false);
    setPostDetails(false);
  };

  const handleQuickShare = (post) => {
    setSinglePost(post);
    setOpenDialog(true);
  };

  const handleShare = async () => {
    setShareLoading(true);
    try {
      const apiMap = {
        "Twitter / X": createXExternalPost,
        Facebook: createFacebookExternalPost,
        Instagram: createInstagramExternalPost,
        Linkedin: createLinkedinExternalPost,
      };
      for (const [platform, isEnabled] of Object.entries(checked)) {
        if (isEnabled) {
          const payload = {
            postId: singlePost?.id,
            type: "POST",
            content: composePlatformContent(
              singlePost?.content || "",
              platform,
              businessPages,
              getSystemHashtags(singlePost)
            ).finalText,
          };

          // Call the correct API for that platform
          const apiFunction = apiMap[platform];
          if (apiFunction) {
            const response = await apiFunction(payload);

            if (response) {
              toast.success(`${platform} post shared successfully`);
              setOpenDialog(false);
            } else {
              toast.error(`${platform} post failed`);
            }
          }
        }
      }
    } catch (error) {
      console.error(error, "error");
      toast.error("Something went wrong while sharing the post");
    } finally {
      setShareLoading(false);
    }
  };

  const handleShareWithThoughts = async (post, e) => {
    e.stopPropagation();
    await prepareThePost(post?.id);
    setOpenSharePopup(true);
  };

  const handleClose = () => {
    setOpenSharePopup(false);
    setPostReviewData(null);
    fetchAllPosts();
  };

  const handleDetails = async (id, e) => {
    e.stopPropagation();
    setPostReviewData(null);
    try {
      // setLoadingPostId(id);
      const response = await postDetailsByPostId(id);
      if (response?.status === true) {
        setPostDetails(true);
        setPostReviewData(response);
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoadingPostId(null);
    }
  };
  const prepareThePost = async (id) => {
    setPostReviewData(null);
    try {
      setLoadingPostId(id);
      const response = await preparePost(`${id}?isShare=true`);
      if (response?.status === true) {
        setPostReviewData(response);
        await summaryThePost(id);
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoadingPostId(null);
    }
  };

  const summaryThePost = async (id) => {
    try {
      const payload = {
        postId: id,
      };
      // const response = await postSummary(payload);
      // if (response?.status === true) {
      //   setSummary(response?.generateContentResponseDTO?.summarizedContent);
      // }
    } catch (error) {
      console.log(error);
    }
  };

  const onCloseCategoriesDialog = () => {
    setOpenCategoriesDialog(false);
  };
  const handleOpenCategoriesDialog = (post) => {
    setSinglePost(post);
    const existingCategoryIds =
      post?.categories?.map((cat) => cat.categoryId) || [];
    setSelectedCategories(existingCategoryIds);
    setOpenCategoriesDialog(true);
  };

  const toggleCategory = (category) => {
    setSelectedCategories((prevSelected) => {
      if (prevSelected.includes(category.id)) {
        // Uncheck: remove from list
        return prevSelected.filter((id) => id !== category.id);
      } else {
        // Check: add to list
        return [...prevSelected, category.id];
      }
    });
  };

  const onSave = async (selected) => {
    const payload = {
      categoryIds: selectedCategories,
      postId: singlePost?.id,
    };
    try {
      const response = await assignCategoryToPost(payload);
      if (response?.status === true) {
        toast.success("Categories assigned successfully!");
      }
      setOpenCategoriesDialog(false);
    } catch (error) {
      console.log(error, "error");
    } finally {
      fetchAllPosts();
    }
  };

  // const fetchAllPosts = async () => {
  //   try {
  //     const payload = {
  //       category: id,
  //       platform: selected.toUpperCase(),
  //       search: categoryList?.searchValue,
  //     };
  //     const response = await getPosts(payload, page, limit);
  //     if (response?.status) {
  //       setPostsList(response?.posts);
  //     }
  //   } catch (error) {
  //     console.log(error);
  //   } finally {
  //   }
  // };
  // Feature 1: Hardcoded demo category tabs
  useEffect(() => {
    setCategoryTabs([
      { id: null, name: "All" },
      { id: 1, name: "Careers" },
      { id: 2, name: "Product Updates" },
      { id: 3, name: "Thought Leadership" },
      { id: 4, name: "Hiring" },
    ]);
  }, []);

  // Feature 7/9: Hardcoded demo featured posts
  useEffect(() => {
    setFeaturedPosts([
      {
        id: "demo-1",
        content: "🚀 Exciting news! We just launched our new product feature that helps teams collaborate 10x faster. Read more about how we're transforming the way people work together.",
        sourceType: "BUSINESS_PAGE",
        sourceName: "Payoneer",
        isFeatured: true,
        isEditable: true,
        featuredUntil: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
        media: [],
      },
      {
        id: "demo-2",
        content: "💡 Leadership insight from our CEO: 'The best companies aren't built on ideas alone — they're built on the courage to execute those ideas even when the path is uncertain.'",
        sourceType: "LEADER_PROFILE",
        sourceName: "Sarah Johnson, CEO",
        isFeatured: true,
        isEditable: false,
        featuredUntil: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
        media: [],
      },
    ]);
  }, []);

  // Feature 7: Demo feature/unfeature (local state only)
  const handleFeaturePost = () => {
    if (!featureTarget) return;
    const until = new Date();
    until.setDate(until.getDate() + featuredUntilDays);
    setFeaturedPosts((prev) =>
      featureTarget.isFeatured
        ? prev.filter((p) => p.id !== featureTarget.id)
        : [...prev, { ...featureTarget, isFeatured: true, featuredUntil: until.toISOString() }]
    );
    setFeatureDialogOpen(false);
    setFeatureTarget(null);
    toast.success(featureTarget.isFeatured ? "Post unfeatured." : "Post featured!");
  };

  // Feature 4: Demo toggle editable (local state only)
  const handleToggleEditable = (post) => {
    setPostsList((prev) =>
      prev.map((p) => p.id === post.id ? { ...p, isEditable: !p.isEditable } : p)
    );
    toast.success(post.isEditable ? "Post locked to quick-share only." : "Post is now editable.");
  };

  const fetchAllPosts = async (newPage = 0) => {
    try {
      setLoading(true);
      // Feature 1: use categoryId from selected tab instead of platform
      const selectedTab = categoryTabs.find((c) => c.name === selected);
      const payload = {
        category: selectedTab?.id || id || undefined,
        search: categoryList?.searchValue,
      };
      const response = await getPosts(payload, newPage, limit);

      if (response?.status) {
        const newPosts = response?.posts || [];
        if (newPage === 0) {
          setPostsList(newPosts);
        } else {
          setPostsList((prev) => [...prev, ...newPosts]);
        }
        // Check if more data is available
        setHasMore(newPosts.length === limit);
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    setPostsList([]);
    setPage(0);
    fetchAllPosts(0);
  }, [
    categoryList?.categoriesDetails,
    selected,
    location,
    categoryList?.searchValue,
  ]);
  useEffect(() => {
    const handleScroll = () => {
      if (
        window.innerHeight + window.scrollY >=
        document.body.offsetHeight - 100
      ) {
        if (!loading && hasMore) {
          const nextPage = page + 1;
          setPage(nextPage);
          fetchAllPosts(nextPage);
        }
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [page, hasMore, loading]);

  // useEffect(() => {
  //   fetchAllPosts();
  // }, [
  //   categoryList?.categoriesDetails,
  //   selected,
  //   location,
  //   categoryList?.searchValue,
  // ]);
  const clear = () => {};
  return (
    <>
      {postReviewData && postDetails && (
        <PostDetails
          open={postDetails}
          postReviewData={postReviewData}
          onClose={onClose}
        />
      )}

      {openSharePopup && postReviewData && (
        <SharePostPopup
          open={openSharePopup}
          title="Share Post"
          type="share"
          selected={"All"}
          onClose={handleClose}
          clear={clear}
          postReviewData={postReviewData}
          summary={summary}
        />
      )}
      <Box
        sx={{
          backgroundColor: "#FFFFFF",
          padding: "8px",
          borderRadius: "16px",
          display: "flex",
          alignItems: "center",
          overflowX: {
            xs: "auto",
            sm: "auto",
            md: "visible",
          },
          whiteSpace: {
            xs: "nowrap",
            sm: "nowrap",
            md: "normal",
          },
          "::-webkit-scrollbar": {
            display: "none",
          },
        }}
      >
        <Box
          sx={{
            borderRadius: "12px",
            padding: "4px",
            display: "flex",
            gap: "12px",
            alignItems: "center",
            width: "100%",
          }}
        >
          {/* Feature 1: Category tabs replacing platform tabs */}
          {categoryTabs.map((cat) => (
            <Button
              key={cat.name}
              onClick={() => { setSelected(cat.name); setPage(0); }}
              disableRipple
              sx={{
                textTransform: "none",
                fontWeight: selected === cat.name ? 600 : 400,
                fontSize: "13px",
                color: selected === cat.name ? "#fff" : "#95919D",
                backgroundColor: selected === cat.name ? "#003db3" : "transparent",
                borderRadius: "8px",
                minWidth: "fit-content",
                padding: "8px 16px",
                whiteSpace: "nowrap",
                "&:hover": { backgroundColor: selected === cat.name ? "#003db3" : "#f0f4ff" },
              }}
            >
              {cat.name}
            </Button>
          ))}
        </Box>
      </Box>
      {/* Feature 7/8/9: Featured Posts Section */}
      {featuredPosts.length > 0 && (
        <Box mt={2} mb={1}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1.5 }}>
            <Box sx={{ width: 4, height: 20, bgcolor: "#003db3", borderRadius: 2 }} />
            <Typography variant="subtitle1" fontWeight={700} color="#003db3">Featured Posts</Typography>
          </Box>
          <Box sx={{ display: "flex", gap: 2, overflowX: "auto", pb: 1, "::-webkit-scrollbar": { height: 4 } }}>
            {featuredPosts.map((fp) => {
              const daysLeft = fp.featuredUntil
                ? Math.ceil((new Date(fp.featuredUntil) - new Date()) / 86400000) : null;
              return (
                <Card key={fp.id} sx={{ minWidth: 280, maxWidth: 320, flexShrink: 0, border: "2px solid #003db3", borderRadius: 3, position: "relative" }}>
                  <Box sx={{ position: "absolute", top: 8, right: 8, zIndex: 1 }}>
                    <Chip label={daysLeft !== null ? `${daysLeft}d left` : "Featured"} size="small"
                      sx={{ bgcolor: "#003db3", color: "#fff", fontWeight: 600, fontSize: 11 }} />
                  </Box>
                  {isAdmin && (
                    <Box sx={{ position: "absolute", top: 8, left: 8, zIndex: 1 }}>
                      <Chip label="Unfeature" size="small" clickable
                        onClick={() => { setFeatureTarget({ ...fp, isFeatured: true }); setFeatureDialogOpen(true); }}
                        sx={{ bgcolor: "#ff4444", color: "#fff", fontWeight: 600, fontSize: 11 }} />
                    </Box>
                  )}
                  <CardHeader
                    avatar={<Avatar src={fp.sourceAvatarUrl || fp.createdBy?.profileImageUrl} sx={{ width: 36, height: 36 }} />}
                    title={<Typography variant="body2" fontWeight={600}>{fp.sourceName || fp.createdBy?.name || "Admin"}</Typography>}
                    subheader={fp.sourceType === "LEADER_PROFILE" ? <Chip label="Leader" size="small" sx={{ fontSize: 10, height: 18 }} /> : null}
                    sx={{ pb: 0 }}
                  />
                  <CardContent sx={{ pt: 1 }}>
                    <Typography variant="body2" color="text.secondary" sx={{ display: "-webkit-box", WebkitLineClamp: 3, WebkitBoxOrient: "vertical", overflow: "hidden" }}>
                      {fp.content}
                    </Typography>
                  </CardContent>
                </Card>
              );
            })}
          </Box>
        </Box>
      )}

      {/* Feature 7: Admin dialog to feature a post */}
      <Dialog open={featureDialogOpen} onClose={() => setFeatureDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>{featureTarget?.isFeatured ? "Unfeature Post" : "Feature Post"}</DialogTitle>
        <DialogContent>
          {!featureTarget?.isFeatured ? (
            <Box>
              <Typography variant="body2" mb={2}>How many days should this post be featured?</Typography>
              {[3, 7, 14].map((d) => (
                <Button key={d} variant={featuredUntilDays === d ? "contained" : "outlined"} size="small"
                  onClick={() => setFeaturedUntilDays(d)} sx={{ mr: 1, mb: 1 }}>
                  {d} days
                </Button>
              ))}
            </Box>
          ) : (
            <Typography variant="body2">Remove this post from the Featured section?</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFeatureDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleFeaturePost} sx={{ bgcolor: "#003db3" }}>
            {featureTarget?.isFeatured ? "Unfeature" : "Feature"}
          </Button>
        </DialogActions>
      </Dialog>

      <Box mt={2} className="postCard-container" sx={{ position: "relative", minHeight: 200 }}>
        {loading && postsList.length === 0 && (
          <Box
            sx={{
              position: "absolute",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              backgroundColor: "rgba(255,255,255,0.8)",
              zIndex: 10,
              borderRadius: 2,
            }}
          >
            <CircularProgress size={36} />
          </Box>
        )}
        <Grid container spacing={1.2}>
          {postsList?.length > 0 ? (
            postsList?.map((post, index) => {
              return (
                <Grid item xs={12} sm={6} md={6} lg={4} key={post?.id}>
                  <Card
                    className="postCard"
                    onClick={() => handleCardClick(post)}
                    elevation={0}
                    sx={{
                      height: { md: "470px" },
                      display: "flex",
                      flexDirection: "column",
                      position: "relative",
                    }}
                  >
                    {/* Tag Icon - Top Right Corner */}
                    {loca !== "employees" && (
                      <IconButton
                        sx={{
                          position: "absolute",
                          top: 8,
                          right: 8,
                          zIndex: 1,
                          width: "28px",
                          height: "28px",
                          backgroundColor: "#F4F0FF",
                          "&:hover": {
                            backgroundColor: "#EDE9FE",
                          },
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenCategoriesDialog(post);
                        }}
                      >
                        <SellIcon
                          sx={{ color: "#2563EB", width: "16px", height: "16px" }}
                        />
                      </IconButton>
                    )}
                    <CardHeader
                      sx={{ padding: "14px 10px 5px !important" }}
                      avatar={
                        <Box sx={{ position: "relative", display: "inline-block" }}>
                          <Avatar
                            src={getDisplayAvatar(post)}
                            alt={getDisplayName(post)}
                            sx={{ width: 35, height: 35 }}
                          />
                          {getPlatformIcon(post) ? (
                            <Box
                              component="img"
                              src={getPlatformIcon(post)}
                              alt={getPlatformLabel(post)}
                              sx={{
                                width: 14,
                                height: 14,
                                position: "absolute",
                                bottom: -2,
                                right: -2,
                                borderRadius: "50%",
                                backgroundColor: "#fff",
                                border: "1px solid #E2E8F0",
                                padding: "1px",
                              }}
                            />
                          ) : null}
                        </Box>
                      }
                      action={
                        <Box sx={{ display: "flex", alignItems: "center" }}>
                          <Typography
                            color="#D5D5D5"
                            fontSize={"14px"}
                            sx={{ mr: 1 }}
                          >
                            {post?.time}
                          </Typography>
                        </Box>
                      }
                      title={
                        <Box sx={{ display: "flex", alignItems: "center" }}>
                          <span>{getDisplayName(post)}</span>
                        </Box>
                      }
                      subheader={
                        <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap", mt: 0.25 }}>
                          {getDisplayUsername(post) ? (
                            <Typography
                              component="span"
                              sx={{ color: "#6B7280", fontSize: "12px", fontWeight: 400 }}
                            >
                              {getDisplayUsername(post)}
                            </Typography>
                          ) : null}
                          {post?.createdBy?.teamName ? (
                            <Typography
                              component="span"
                              sx={{
                                color: "#95919D",
                                fontSize: { xs: "11px", sm: "13px", md: "12px" },
                                fontWeight: 400,
                              }}
                            >
                              {post.createdBy.teamName}
                            </Typography>
                          ) : null}
                          {getSourceLabel(post) ? (
                            <Chip
                              size="small"
                              label={getSourceLabel(post)}
                              sx={{
                                height: 20,
                                fontSize: "10px",
                                fontWeight: 600,
                                backgroundColor:
                                  post?.sourceType === "LEADER_PROFILE" ? "#EEF2FF" : "#ECFDF3",
                                color:
                                  post?.sourceType === "LEADER_PROFILE" ? "#3730A3" : "#166534",
                              }}
                            />
                          ) : null}
                        </Box>
                      }
                      titleTypographyProps={{
                        sx: {
                          color: "#3B3B3B",
                          fontWeight: 500,
                          // fontSize: "18px",
                          fontSize: { xs: "12px", sm: "14px", md: "13px" },
                        },
                      }}
                    />
                    <CardContent
                      sx={{
                        padding: "0px 11px 16px 11px !important",
                        flex: 1,
                        display: "flex",
                        flexDirection: "column",
                      }}
                    >
                      <Tooltip title={post?.content} arrow>
                        <Typography
                          color="#5D5D5D"
                          fontWeight={500}
                          onClick={
                            post?.media?.length > 0
                              ? undefined
                              : (e) => handleDetails(post?.id, e)
                          }
                          sx={{
                            fontSize: { xs: "12px", sm: "14px", md: "12px" },
                            lineHeight: "1.5rem",
                            display: "-webkit-box",
                            WebkitLineClamp: post?.media?.length > 0 ? 2 : 13,
                            WebkitBoxOrient: "vertical",
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            cursor: post?.media?.length > 0 ? "default" : "pointer",
                            wordBreak: "break-word",
                            height: post?.media?.length > 0 ? "3rem" : "auto",
                          }}
                          gutterBottom
                          mb={1.5}
                        >
                          {post?.content}{" "}
                          {/* <span style={{ color: "#2D76DC" }}>@Manu George</span> */}
                        </Typography>
                      </Tooltip>

                      {post?.media?.length > 0 && (
                        <Box
                          position="relative"
                          borderRadius="8px"
                          overflow="hidden"
                        >
                          {post?.media
                            .slice(
                              currentImageIndexes?.[post?.id] ?? 0,
                              (currentImageIndexes?.[post?.id] ?? 0) + 1
                            )
                            .map((mediaItem, index) => {
                              const currentMediaIndex =
                                currentImageIndexes?.[post?.id] ?? 0;
                              const isActiveBunnyPlayer =
                                activeBunnyPlayers?.[post?.id] ===
                                currentMediaIndex;

                              return mediaItem?.mediaType === "IMAGE" ? (
                                <CardMedia
                                  key={index}
                                  onClick={(e) => handleDetails(post?.id, e)}
                                  component="img"
                                  image={getResolvedImageUrl(mediaItem, noImage)}
                                  sx={{
                                    height: { md: "280px" },
                                    width: "100%",
                                    objectFit: "cover",
                                    borderRadius: "8px",
                                    cursor: "pointer",
                                  }}
                                />
                              ) : mediaItem?.mediaType === "VIDEO" ? (
                                <Box
                                  key={index}
                                  sx={{
                                    position: "relative",
                                    height: { md: "280px" },
                                    width: "100%",
                                    borderRadius: "8px",
                                    overflow: "hidden",
                                  }}
                                  onClick={(e) => handleDetails(post?.id, e)}
                                >
                                  {isBunnyStreamMedia(mediaItem) ? (
                                    isActiveBunnyPlayer ? (
                                      <iframe
                                        src={getBunnyEmbedUrl(
                                          getResolvedVideoUrl(mediaItem)
                                        )}
                                        title={post?.title || "Timeline video"}
                                        style={{
                                          height: "100%",
                                          width: "100%",
                                          border: 0,
                                          display: "block",
                                          borderRadius: "8px",
                                        }}
                                        allow="accelerometer; gyroscope; encrypted-media; picture-in-picture;"
                                        allowFullScreen
                                      />
                                    ) : (
                                      <>
                                        <CardMedia
                                          component="img"
                                          image={getResolvedPosterUrl(mediaItem, noImage)}
                                          sx={{
                                            height: "100%",
                                            width: "100%",
                                            objectFit: "cover",
                                            borderRadius: "8px",
                                            cursor: "pointer",
                                          }}
                                        />
                                        <IconButton
                                          onClick={(e) =>
                                            handleActivateBunnyPlayer(
                                              post?.id,
                                              currentMediaIndex,
                                              e
                                            )
                                          }
                                          sx={{
                                            position: "absolute",
                                            top: "50%",
                                            left: "50%",
                                            transform: "translate(-50%, -50%)",
                                            backgroundColor: "rgba(0,0,0,0.55)",
                                            color: "#fff",
                                            "&:hover": {
                                              backgroundColor:
                                                "rgba(0,0,0,0.7)",
                                            },
                                          }}
                                        >
                                          <PlayArrowIcon />
                                        </IconButton>
                                      </>
                                    )
                                  ) : (
                                    <video
                                      src={getResolvedVideoUrl(mediaItem)}
                                      poster={getResolvedPosterUrl(mediaItem)}
                                      style={{
                                        height: "100%",
                                        width: "100%",
                                        objectFit: "cover",
                                        borderRadius: "8px",
                                        cursor: "pointer",
                                      }}
                                      controls
                                    />
                                  )}
                                </Box>
                              ) : null
                            })}
                          {/* <CardMedia
                          onClick={(e) => handleDetails(post?.id, e)}
                          component="img"
                          image={
                            post?.media?.length > 0 &&
                            post?.media?.[currentImageIndexes?.[post?.id] ?? 0]
                              ?.fileUrl
                              ? post?.media?.[
                                  currentImageIndexes?.[post?.id] ?? 0
                                ]?.fileUrl
                              : noImage
                          }
                          // alt={`Image ${post?.content}`}
                          sx={{
                            height: { md: "280px" },
                            width: "100%",
                            objectFit: "cover",
                            borderRadius: "8px",
                            cursor: "pointer",
                          }}
                        /> */}

                          {post?.media?.length > 1 && (
                            <>
                              <IconButton
                                onClick={() =>
                                  handlePrev(post?.id, post?.media?.length)
                                }
                                sx={{
                                  position: "absolute",
                                  top: "90%",
                                  left: 12,
                                  transform: "translateY(-50%)",
                                  opacity: "50%",
                                  width: "20px",
                                  height: "20px",
                                  backgroundColor: "#fff",
                                  color: "#000",
                                  "&:hover": {
                                    backgroundColor: "#fff",
                                  },
                                }}
                              >
                                <ChevronLeftIcon
                                  sx={{ width: "20px", height: "20px" }}
                                />
                              </IconButton>

                              <IconButton
                                onClick={() =>
                                  handleNext(post?.id, post?.media?.length)
                                }
                                sx={{
                                  position: "absolute",
                                  top: "90%",
                                  right: 12,
                                  transform: "translateY(-50%)",
                                  backgroundColor: "#fff",
                                  opacity: "50%",
                                  width: "20px",
                                  height: "20px",
                                  color: "#000",
                                  "&:hover": {
                                    backgroundColor: "#fff",
                                  },
                                }}
                              >
                                <ChevronRightIcon
                                  sx={{ width: "20px", height: "20px" }}
                                />
                              </IconButton>
                              <Box
                                sx={{
                                  position: "absolute",
                                  bottom: 25,
                                  left: "48%",
                                  transform: "translateX(-50%)",
                                  display: "flex",
                                  gap: 1,
                                }}
                              >
                                {post?.media?.map((_, index) => (
                                  <Box
                                    key={index}
                                    sx={{
                                      width: 8,
                                      height: 8,
                                      borderRadius: "50%",
                                      backgroundColor:
                                        index ===
                                        (currentImageIndexes[post.id] ?? 0)
                                          ? "primary.main"
                                          : "#F2F2F2",
                                    }}
                                  />
                                ))}
                              </Box>
                            </>
                          )}
                        </Box>
                      )}
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "center",
                          alignItems: "center",
                          flexDirection: {
                            xs: "column", // mobile aur tablet: upar-neche
                            sm: "column",
                            md: "column",
                            lg: "row", // large screens: ek row
                            xl: "row",
                          },
                          flexWrap: {
                            xs: "wrap",
                            sm: "wrap",
                            md: "wrap",
                            lg: "nowrap",
                            // xl: "nowrap",
                          },
                          gap: { xs: 1, sm: 1.5, md: 0.7 }, // responsive spacing
                          mt: "auto",
                          pt: 2,
                        }}
                      >
                        <Button
                          startIcon={
                            <img
                              src={edit}
                              alt="edit"
                              style={{
                                width: "15px",
                                height: "15px",
                              }}
                            />
                          }
                          sx={{
                            textTransform: "none",
                            fontWeight: 400,
                            fontSize: { xs: "12px", sm: "13px", md: "11px" }, // responsive font size
                            color: "#0047AB",
                            borderColor: "#f1efff",
                            width: "100%",
                            backgroundColor: "#F4F0FF",
                            borderRadius: "10px",
                            padding: {
                              xs: "8px 12px",
                              sm: "10px 14px",
                              md: "10px 10px",
                              lg: "10px 5px",
                            },
                            "&:hover": {
                              backgroundColor: "#f1efff",
                              borderColor: "#f1efff",
                            },
                            "& .MuiButton-startIcon": {
                              marginRight: "4px",
                            },
                          }}
                          onClick={(e) => handleShareWithThoughts(post, e)}
                          disabled={loadingPostId === post?.id}
                        >
                          {loadingPostId === post?.id ? (
                            <CircularProgress size={20} color="inherit" />
                          ) : (
                            "Share with Thoughts"
                          )}
                        </Button>

                        <Button
                          onClick={() => handleQuickShare(post)}
                          startIcon={
                            <img
                              src={share}
                              alt="share"
                              style={{ width: "16px", height: "16px" }}
                            />
                          }
                          sx={{
                            textTransform: "none",
                            fontWeight: 400,
                            fontSize: { xs: "13px", sm: "14px", md: "11px" },
                            backgroundColor: "#003db3",
                            borderRadius: "10px",
                            width: {
                              xs: "100%",
                              sm: "100%",
                              md: "90%",
                              lg: "75%",
                            },
                            color: "#fff",
                            padding: {
                              xs: "8px 18px",
                              sm: "10px 22px",
                              md: "10px 0px",
                              lg: "10px 0px",
                            },
                            "&:hover": {
                              backgroundColor: "#002e80",
                            },
                          }}
                        >
                          Quick Share
                        </Button>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })
          ) : (
            <Box
              display={"flex"}
              justifyContent={"center"}
              width={"100%"}
              mt={5}
              height={"100px"}
            >
              {(() => {
                const platformKey = platformKeyMap[selected];
                const isConnected = selected === "Leadership" || !!businessPages?.[platformKey];
                if (isConnected) {
                  return (
                    <Typography variant="body2" align="center" color="#95919D">
                      No posts found.
                    </Typography>
                  );
                }
                return (
                  <Box textAlign="center">
                    <Typography variant="body2" color="#95919D">
                      {selected} is not connected.
                    </Typography>
                    <Typography
                      variant="body2"
                      component={Link}
                      to={isAdmin ? "/admin/settings" : "/employees/settings"}
                      sx={{ color: "#2D76DC", cursor: "pointer", mt: 1, display: "block", textDecoration: "none" }}
                    >
                      Go to Settings to connect
                    </Typography>
                  </Box>
                );
              })()}
            </Box>
          )}
        </Grid>
        {loading && (
          <Box textAlign="center" mt={2}>
            <CircularProgress size={24} />
          </Box>
        )}
        {/* Menu for Edit/Delete */}
        <Menu
          anchorEl={menuAnchor}
          open={Boolean(menuAnchor)}
          onClose={handleMenuClose}
          anchorOrigin={{ vertical: "top", horizontal: "right" }}
          transformOrigin={{ vertical: "top", horizontal: "right" }}
        >
          <MenuItem onClick={() => alert(`Edit Post #${selectedPost + 1}`)}>Edit</MenuItem>
          <MenuItem onClick={() => alert(`Delete Post #${selectedPost + 1}`)}>Delete</MenuItem>
          {isAdmin && (() => {
            const post = postsList[selectedPost];
            if (!post) return null;
            return [
              <MenuItem key="feature" onClick={() => {
                setFeatureTarget(post);
                setFeatureDialogOpen(true);
                handleMenuClose();
              }}>
                {post.isFeatured ? "⭐ Unfeature" : "⭐ Feature Post"}
              </MenuItem>,
              <MenuItem key="editable" onClick={() => { handleToggleEditable(post); handleMenuClose(); }}>
                {post.isEditable === false ? "🔓 Allow Editing" : "🔒 Lock (Quick-Share Only)"}
              </MenuItem>,
            ];
          })()}
        </Menu>
      </Box>

      <PostDailog
        isOpen={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
        post={clickedPost}
      />

      <Dialog
        open={openDialog}
        onClose={onClose}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: { borderRadius: "16px", px: 2, py: 1 },
        }}
      >
        <DialogTitle
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            pb: 1,
            padding: "15px",
          }}
        >
          <Typography variant="h6" color="#484848" fontWeight="500">
            Select Platforms For Quick Sharing
          </Typography>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            {platform?.map((platform) => (
              <Box
                key={platform.name}
                sx={{
                  backgroundColor: "#F8F6FD",
                  borderRadius: "12px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  px: 2,
                  py: 1.5,
                }}
              >
                <Box display="flex" alignItems="center" gap={2}>
                  <Avatar
                    sx={{
                      bgcolor: "#fff",
                      width: 32,
                      height: 32,
                    }}
                    src={platform?.icon}
                    alt={platform?.name}
                  ></Avatar>
                  <Typography fontSize="16px" color="#5C5B5D" fontWeight="400">
                    {platform?.name}
                  </Typography>
                </Box>
                <Switch
                  checked={checked[platform?.name]}
                  onChange={() => handleToggle(platform?.name)}
                  color="primary"
                />
              </Box>
            ))}
          </Stack>

          <Box mt={4} display="flex" justifyContent="end" gap={2}>
            <Button
              onClick={onClose}
              disabled={shareLoading}
              sx={{
                backgroundColor: "#F8F6FD",
                color: "#003db3",
                borderRadius: "12px",
                textTransform: "none",
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
              onClick={handleShare}
              variant="contained"
              disabled={shareLoading}
              sx={{
                backgroundColor: "#003db3",
                color: "#fff",
                borderRadius: "12px",
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
              {shareLoading ? "Sharing..." : "Share"}
            </Button>
          </Box>
        </DialogContent>
      </Dialog>

        <Dialog
          open={openCategoriesDialog}
          onClose={onCloseCategoriesDialog}
        PaperProps={{
          sx: {
            borderRadius: 3,
            p: 2,
            minWidth: 400,
          },
        }}
      >
        <DialogTitle
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            pb: 1,
            padding: "15px",
          }}
        >
          <Typography variant="h6" color="#484848" fontWeight="500">
            Select Categories
          </Typography>
          <IconButton onClick={onCloseCategoriesDialog}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>
          <List sx={{ maxHeight: 300, overflowY: "auto" }}>
            {categoryList?.categoriesData?.map((category) => {
              const checked = selectedCategories.includes(category.id);

              return (
                <ListItem
                  key={category.id}
                  onClick={() => toggleCategory(category)}
                  sx={{
                    mb: 1,
                    px: 2,
                    py: 1.5,
                    borderRadius: 2,
                    bgcolor: checked ? "#f8f6fd" : "#f8f6fd",
                    cursor: "pointer",
                    transition: "0.2s",
                    "&:hover": { bgcolor: "#F1F5F9" },
                    display: "flex",
                    justifyContent: "space-between",
                  }}
                >
                  <ListItemText primary={category?.name} />
                  <Checkbox
                    checked={checked}
                    sx={{
                      color: "#2563EB",
                      "&.Mui-checked": {
                        color: "#2563EB",
                      },
                    }}
                  />
                </ListItem>
              );
            })}
          </List>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={onCloseCategoriesDialog}
            sx={{
              backgroundColor: "#F8F6FD",
              color: "#003db3",
              borderRadius: "12px",
              textTransform: "none",
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
            onClick={() => onSave(selected)}
            sx={{
              backgroundColor: "#003db3",
              color: "#fff",
              borderRadius: "12px",
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
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default PostCards;
