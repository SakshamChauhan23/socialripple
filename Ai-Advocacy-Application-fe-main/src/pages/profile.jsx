import React, { useEffect, useRef, useState } from "react";
import {
  Card,
  CardMedia,
  Typography,
  Avatar,
  Box,
  Grid,
  Tabs,
  Tab,
  Button,
  Dialog,
  IconButton,
  DialogContent,
  TextField,
  DialogTitle,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import "../components/Layout/Teams/TeamDetails/Teamdetails.css";
import facebook from "../assets/socialmedia/facebook.svg";
import insta from "../assets/socialmedia/insta.svg";
import linkedin from "../assets/socialmedia/linkedin.svg";
import X from "../assets/socialmedia/twiiter.svg";
import coins from "../assets/socialmedia/coins.svg";
import share from "../assets/profile/share.png";
import instagram1 from "../assets/insta.png";
import linkedin1 from "../assets/linkedin.png";
import facebook1 from "../assets/facebook.png";
import twiter1 from "../assets/twitter.png";
import logout from "../assets/logout.png";
import CloseIcon from "@mui/icons-material/Close";
import noImage from "../assets/noimage.jpg";
import DeleteIcon from "@mui/icons-material/Delete";
import uploadp from "../assets/uploadp.png";
import UserService from "../services/categoryService";
import { toast } from "react-toastify";
import { Auth } from "../contexts/AuthContext";
import { postDetailsByPostId, uploadMedia } from "../services/postService";
import PostDetails from "../components/Layout/Postcards/PostDetails";
import PostMediaViewer from "../components/Layout/Teams/TeamDetails/mediaListing";
const ProfileDetails = () => {
  const fileInputRef = useRef(null);
  const [tab, setTab] = useState(0);
  const [open, setOpen] = useState(false);
  const [open1, setOpen1] = useState(false);
  const [loader, setLoader] = useState(false);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState(null);
  const [profile, setProfile] = useState({});
  const { loyaltyPoints, setLoyaltyPoints } = Auth();
  const [imageIds, setImageIds] = useState([]);
  const [image, setImage] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [platformCount, setPlatformCount] = useState(null);
  const [posts, setPosts] = useState([]);
  const [postsCount, setPostsCount] = useState(0);
  const [sharesCount, setSharesCount] = useState(0);
  const [tagsCount, setTagsCount] = useState(0);
  const [postDetails, setPostDetails] = useState(false);
  const [postReviewData, setPostReviewData] = useState(null);
  const [updateLoading, setUpdateLoading] = useState(false);
  const onClose = () => {
    setOpen(false);
    setOpen1(false);
  };
  const handleOpen = () => {
    setImage(profile?.profilePicture || "");
    setOpen(true);
  };
  const handleLogout = () => {
    setOpen1(true);
  };
  const onConfirm = () => {
    sessionStorage.clear();
    window.location.href = "/sign-in";
    setOpen1(false);
  };
  const fetchLoyaltyPoints = async () => {
    try {
      const response = await UserService.getLoyaltyPoints();
      if (response?.status === true) {
        setLoyaltyPoints(response?.loyalty);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchCounting = async () => {
    try {
      const response = await UserService.getCounting();
      if (response?.status === true) {
        setPlatformCount(response?.platformCountMap);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchProfile = async () => {
    setProfileLoading(true);
    setProfileError(null);
    try {
      const response = await UserService.getProfile();
      if (response?.status === true) {
        setProfile(response?.data);
        setName(response?.data?.name);
        setPhone(response?.data?.phone || "");
        setImage(response?.data?.profilePicture || "");
      } else {
        setProfileError("Failed to load profile data.");
      }
    } catch (error) {
      console.log(error);
      setProfileError("Unable to load profile. Please try again.");
    } finally {
      setProfileLoading(false);
    }
  };

  const fetchSharesCount = async () => {
    try {
      const response = await UserService.getShares();
      if (response?.status === true) {
        setSharesCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {}
  };
  const fetchTagsCount = async () => {
    try {
      const response = await UserService.getTags();
      if (response?.status === true) {
        setTagsCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {}
  };

  useEffect(() => {
    fetchProfile();
    fetchActivities();
    fetchSharesCount();
    fetchTagsCount();
    fetchCounting();
    fetchLoyaltyPoints();
  }, []);

  const handleUpdateData = async () => {
    setUpdateLoading(true);
    try {
      const payload = {
        name: name,
        phone: phone,
        profilePicture: imageIds.length > 0 ? imageIds[0].fileUrl : profile?.profilePicture,
      };
      const response = await UserService.updateProfile(payload);
      if (response?.status === true) {
        fetchProfile();
        toast.success("Profile Updated Successfully");
      }
    } catch (error) {
      console.log(error);
    } finally {
      setOpen(false);
      setLoader(false);
      setUpdateLoading(false);
    }
  };

  const handleFileUpload = async (file) => {
    const reader = new FileReader();
    const formData = new FormData();
    formData.append("files", file);
    formData.append("mediaType", "image");
    try {
      const response = await uploadMedia(formData);
      if (response?.status === true) {
        setImageIds(response?.mediaUploadData?.mediaDTOS);
        reader.onload = (e) => setImage(e.target.result);
        reader.readAsDataURL(file);
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
    }
  };

  const fetchActivities = async () => {
    try {
      const response = await UserService.getActivities();
      if (response?.status === true) {
        setPosts(response?.posts);
        setPostsCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchShares = async () => {
    try {
      const response = await UserService.getShares();
      if (response?.status === true) {
        setPosts(response?.posts);
        setSharesCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchTags = async () => {
    try {
      const response = await UserService.getTags();
      if (response?.status === true) {
        setPosts(response?.posts);
        setTagsCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const setupTabs = async (v) => {
    console.log(v);
    try {
      if (v === 0) {
        setPosts([]);
        await fetchActivities();
        setTab(v);
      } else if (v === 1) {
        setPosts([]);
        await fetchShares();
        setTab(v);
      } else {
        setPosts([]);
        await fetchTags();
        setTab(v);
      }
    } catch (error) {
      console.log(error, "errors");
    }
  };

  const onCloses = () => {
    setPostDetails(false);
  };
  const handleDetails = async (id, e) => {
    e.stopPropagation();
    setPostReviewData(null);
    try {
      const response = await postDetailsByPostId(id);
      if (response?.status === true) {
        setPostDetails(true);
        setPostReviewData(response);
      }
    } catch (error) {
      console.log(error);
    }
  };

  if (profileLoading) {
    return (
      <Card sx={{ height: "100%", display: "flex", justifyContent: "center", alignItems: "center", p: 4 }}>
        <Typography color="text.secondary">Loading profile...</Typography>
      </Card>
    );
  }

  if (profileError) {
    return (
      <Card sx={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", p: 4, gap: 2 }}>
        <Typography color="error">{profileError}</Typography>
        <Button variant="outlined" onClick={fetchProfile}>Retry</Button>
      </Card>
    );
  }

  return (
    <>
      {postReviewData && postDetails && (
        <PostDetails
          open={postDetails}
          postReviewData={postReviewData}
          onClose={onCloses}
        />
      )}
      <Card
        sx={{ height: "100%" }}
        elevation={0}
        className="teamsDeatail-container"
      >
        {/* Profile Section */}
        <Box
          className="team-member-profile"
          sx={{
            display: "flex",
            flexDirection: { xs: "column", md: "row" },
            justifyContent: "space-between",
            alignItems: { xs: "center", md: "flex-start" },
            gap: { xs: 3, md: 0 },
          }}
        >
          {/* Left - Avatar & Name */}
          <Box
            display="flex"
            alignItems={{ xs: "center", md: "center" }}
            flexDirection={{ xs: "column", md: "row" }}
            textAlign={{ xs: "center", md: "left" }}
          >
            <Avatar
              sx={{
                bgcolor: "grey.300",
                border: "4px solid white",
                width: 100,
                height: 100,
              }}
              src={profile?.profilePicture || ""}
            />
            {/* <Box ml={2}> */}
            <Box ml={{ xs: 0, md: 2 }} mt={{ xs: 2, md: 0 }}>
              <Typography
                variant="h6"
                color="#FFFFFF"
                fontSize={{ xs: "20px", sm: "26px", md: "28px" }}
                fontWeight="600"
              >
                {profile?.name || ""}
              </Typography>
              <Typography
                variant="body2"
                fontWeight="400"
                fontSize={{ xs: "14px", sm: "16px", md: "16px" }}
                color="#E7E7E7"
              >
                {[profile?.jobTitle, profile?.teamName].filter(Boolean).join(" | ")}
              </Typography>
              <Box
                display="flex"
                justifyContent={{ xs: "center", md: "flex-start" }}
                alignItems="center"
                gap={2}
                mt={1}
                flexWrap="wrap"
              >
                <Button
                  variant="outlined"
                  startIcon={
                    <EditIcon
                      sx={{ color: "white", fontSize: "13px !important" }}
                    />
                  }
                  sx={{
                    borderColor: "white",
                    color: "white",
                    borderRadius: "16px",
                    padding: { xs: "2px 12px", sm: "4px 16px", md: "4px 20px" },
                    textTransform: "none",
                    fontSize: { xs: "10px", md: "10px" },
                    fontWeight: 400,
                    "&:hover": {
                      borderColor: "white",
                      backgroundColor: "rgba(255,255,255,0.1)",
                    },
                  }}
                  onClick={handleOpen}
                >
                  Edit profile
                </Button>

                <Button
                  variant="outlined"
                  startIcon={
                    <img
                      src={logout}
                      style={{ color: "white", width: "12px", height: "12px" }}
                    />
                  }
                  sx={{
                    borderColor: "white",
                    color: "white",
                    borderRadius: "16px",
                    padding: { xs: "2px 12px", sm: "4px 16px", md: "4px 20px" },
                    textTransform: "none",
                    fontSize: { xs: "10px", md: "10px" },
                    fontWeight: 400,
                    "&:hover": {
                      borderColor: "white",
                      backgroundColor: "rgba(255,255,255,0.1)",
                    },
                  }}
                  onClick={handleLogout}
                >
                  Logout
                </Button>
              </Box>
            </Box>
          </Box>

          {/* Right - Coin count & Social Media Stats */}
          {/* <Box display="flex" flexDirection="column" alignItems="flex-end"> */}
          <Box
            display="flex"
            flexDirection="column"
            alignItems={{ xs: "center", md: "flex-end" }}
            mt={{ xs: 3, md: 0 }}
            width={{ xs: "100%", md: "auto" }}
          >
            {/* Loyalty Points */}
            <Box display="flex" alignItems="center" mb={2}>
              <img src={coins} alt="coins" width={23} height={23} />
              <Typography
                variant="h6"
                fontSize={{ xs: "18px", sm: "20px", md: "20px" }}
                fontWeight="500"
                ml={1}
              >
                {loyaltyPoints?.loyaltyPoints || 0}
              </Typography>
              <Typography
                variant="body2"
                fontSize={{ xs: "11px", md: "12px" }}
                fontWeight="400"
                color="#E7E7E7"
                ml={1}
              >
                Loyalty Points
              </Typography>
            </Box>

            {/* Platform Share Counts */}
            <Typography
              variant="body2"
              fontSize={{ xs: "11px", md: "12px" }}
              fontWeight="400"
              color="#E7E7E7"
              mb={0.5}
              textAlign={{ xs: "center", md: "right" }}
            >
              Shares
            </Typography>
            <Box
              display="flex"
              alignItems="center"
              gap={{ xs: 2, sm: 3, md: 2 }}
              flexWrap="wrap"
              justifyContent={{ xs: "center", md: "flex-end" }}
            >
              <Box display="flex" alignItems="center">
                <img src={facebook} alt="facebook" width={20} height={20} />
                <Typography
                  variant="body2"
                  fontSize={{ xs: "16px", sm: "18px", md: "18px" }}
                  marginLeft={1}
                >
                  {platformCount?.FACEBOOK || 0}
                </Typography>
              </Box>
              <Box display="flex" alignItems="center">
                <img src={insta} alt="insta" width={20} height={20} />
                <Typography
                  variant="body2"
                  fontSize={{ xs: "16px", sm: "18px", md: "18px" }}
                  marginLeft={1}
                >
                  {platformCount?.INSTAGRAM || 0}
                </Typography>
              </Box>
              <Box display="flex" alignItems="center">
                <img src={linkedin} alt="linkedin" width={20} height={20} />
                <Typography
                  variant="body2"
                  fontSize={{ xs: "16px", sm: "18px", md: "18px" }}
                  marginLeft={1}
                >
                  {platformCount?.LINKEDIN || 0}
                </Typography>
              </Box>
              <Box display="flex" alignItems="center">
                <img src={X} alt="x" width={20} height={20} />
                <Typography
                  variant="body2"
                  fontSize={{ xs: "16px", sm: "18px", md: "18px" }}
                  marginLeft={1}
                >
                  {platformCount?.X || 0}
                </Typography>
              </Box>
            </Box>
          </Box>
        </Box>

        <Box>
          <Box mb={3} mt={2} width={{ xs: "100%", md: "53%" }}>
            <Tabs
              value={tab}
              onChange={(e, v) => setupTabs(v)}
              variant="fullWidth"
              TabIndicatorProps={{ style: { display: "none" } }}
              sx={{
                backgroundColor: "#F5F5F9",
                borderRadius: "50px",
                pt: 1,
                pb: 1,
                mb: 2,
                pl: 2,
                pr: 2,
                color: "#3B3B3B",
                "& .MuiTab-root": {
                  textTransform: "none",
                  color: "#3B3B3B",
                  fontWeight: 400,
                  fontSize: "14px",
                },
                "& .Mui-selected": {
                  color: "#3B3B3B !important",
                  background: "#fff",
                  fontSize: "14px",
                  borderRadius: "20px",
                  fontWeight: 600,
                },
              }}
            >
              <Tab
                label={
                  <Box display={"flex"} alignItems={"center"} gap={1}>
                    <div>
                      Posts&nbsp;(
                      <span style={{ color: "#2D76DC" }}>
                        {postsCount}
                      </span>
                      )
                    </div>
                  </Box>
                }
              />
              <Tab
                label={
                  <Box>
                    Shares&nbsp; (
                    <span style={{ color: "#2D76DC" }}>
                      {sharesCount}
                    </span>
                    )
                  </Box>
                }
              />
              <Tab
                label={
                  <Box>
                    Tags&nbsp; (
                    <span style={{ color: "#2D76DC" }}>
                      {" "}
                      {tagsCount}
                    </span>
                    )
                  </Box>
                }
              />
            </Tabs>
          </Box>
        </Box>

        {/* Posts Section */}
        {tab === 0 && (
          <Box mt={3} sx={{ backgroundColor: "#F8F8FA", borderRadius: 2, p: 1.5 }}>
            <Box
              sx={{
                maxHeight: "80vh",
                overflowY: "auto",
                pr: 1,
                // Styled scrollbar - Chrome, Safari, Edge
                "&::-webkit-scrollbar": {
                  width: "6px",
                },
                "&::-webkit-scrollbar-track": {
                  backgroundColor: "#f1f1f1",
                  borderRadius: "10px",
                },
                "&::-webkit-scrollbar-thumb": {
                  backgroundColor: "#c1c1c1",
                  borderRadius: "10px",
                  "&:hover": {
                    backgroundColor: "#a1a1a1",
                  },
                },
                // Firefox
                scrollbarWidth: "thin",
                scrollbarColor: "#c1c1c1 #f1f1f1",
              }}
            >
              {posts?.length !== 0 ? (
                <Grid container spacing={1}>
                  {posts?.map((post) => (
                    <Grid item xs={12} sm={6} md={6} lg={4} key={post?.id}>
                      <Card
                        elevation={0}
                        onClick={(e) => handleDetails(post?.id, e)}
                        sx={{
                          boxShadow: "none",
                          borderRadius: "12px",
                          border: "1px solid #F0F0F0",
                          cursor: "pointer",
                          "&:hover": { boxShadow: "0 2px 8px rgba(0,0,0,0.06)" },
                        }}
                      >
                        <Box sx={{ padding: "8px 12px 4px 12px" }}>
                          <Typography
                            color="#5D5D5D"
                            fontWeight={500}
                            sx={{
                              fontSize: "12px",
                              lineHeight: "1.4rem",
                              display: "-webkit-box",
                              WebkitLineClamp: post?.media?.length > 0 ? 2 : 6,
                              WebkitBoxOrient: "vertical",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              wordBreak: "break-word",
                              minHeight: post?.media?.length > 0 ? undefined : "60px",
                            }}
                          >
                            {post?.content || post?.title || ""}
                          </Typography>
                        </Box>
                        {post?.media?.length > 0 && (
                          <PostMediaViewer media={post?.media || []} />
                        )}
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            padding: "6px 12px",
                            borderTop: "1px solid #F5F5F5",
                          }}
                        >
                          <Box display="flex" alignItems="center" gap={0.6}>
                            <img
                              src={share}
                              alt="share"
                              style={{ width: "12px", height: "12px" }}
                            />
                            <Typography fontSize="11px" color="#5D5D5D">
                              {post?.postShareData?.shareCount || 0} shares
                            </Typography>
                          </Box>
                          <Box display="flex" gap={1} alignItems="center">
                            {[
                              { key: "hasInstagram", src: instagram1 },
                              { key: "hasLinkedin", src: linkedin1 },
                              { key: "hasFacebook", src: facebook1 },
                              { key: "hasX", src: twiter1 },
                            ]
                              .filter((item) => post?.postShareData?.[item.key])
                              .map((item, idx) => (
                                <Avatar
                                  key={idx}
                                  alt={item.key}
                                  sx={{ width: 16, height: 16 }}
                                  src={item.src}
                                />
                              ))}
                          </Box>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Typography
                  textAlign={"center"}
                  variant="body1"
                  color="#95919D"
                >
                  No data found
                </Typography>
              )}
            </Box>
          </Box>
        )}

        {tab === 1 && (
          <Box mt={3} sx={{ backgroundColor: "#F8F8FA", borderRadius: 2, p: 1.5 }}>
            <Box
              sx={{
                maxHeight: "80vh",
                overflowY: "auto",
                pr: 1,
                // Styled scrollbar - Chrome, Safari, Edge
                "&::-webkit-scrollbar": {
                  width: "6px",
                },
                "&::-webkit-scrollbar-track": {
                  backgroundColor: "#f1f1f1",
                  borderRadius: "10px",
                },
                "&::-webkit-scrollbar-thumb": {
                  backgroundColor: "#c1c1c1",
                  borderRadius: "10px",
                  "&:hover": {
                    backgroundColor: "#a1a1a1",
                  },
                },
                // Firefox
                scrollbarWidth: "thin",
                scrollbarColor: "#c1c1c1 #f1f1f1",
              }}
            >
              {posts?.length !== 0 ? (
                <Grid container spacing={1.2}>
                  {posts.map((post) => (
                    <Grid item xs={12} sm={6} md={6} lg={4} key={post}>
                      <Card
                        elevation={0}
                        onClick={(e) => handleDetails(post?.id, e)}
                        sx={{
                          position: "relative",
                          boxShadow: "none",
                          borderRadius: "16px",
                          display: "flex",
                          flexDirection: "column",
                        }}
                      >
                        <Box sx={{ padding: "8px 12px 4px 12px" }}>
                          <Typography
                            color="#5D5D5D"
                            fontWeight={500}
                            sx={{
                              fontSize: "12px",
                              lineHeight: "1.4rem",
                              display: "-webkit-box",
                              WebkitLineClamp: post?.media?.length > 0 ? 2 : 6,
                              WebkitBoxOrient: "vertical",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              wordBreak: "break-word",
                              minHeight: post?.media?.length > 0 ? undefined : "60px",
                            }}
                          >
                            {post?.content || post?.title || ""}
                          </Typography>
                        </Box>
                        {post?.media?.length > 0 && (
                          <PostMediaViewer media={post?.media || []} />
                        )}
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            padding: "6px 12px",
                            borderTop: "1px solid #F5F5F5",
                            mt: "auto",
                          }}
                        >
                          <Box display="flex" alignItems="center" gap={0.6}>
                            <img src={share} alt="share" style={{ width: "12px", height: "12px" }} />
                            <Typography fontSize="11px" color="#5D5D5D">
                              {post?.postShareData?.shareCount || 0} shares
                            </Typography>
                          </Box>
                          <Box display="flex" gap={1} alignItems="center">
                            {[
                              { key: "hasInstagram", src: instagram1 },
                              { key: "hasLinkedin", src: linkedin1 },
                              { key: "hasFacebook", src: facebook1 },
                              { key: "hasX", src: twiter1 },
                            ]
                              .filter((item) => post?.postShareData?.[item.key])
                              .map((item, idx) => (
                                <Avatar key={idx} alt={item.key} sx={{ width: 18, height: 18 }} src={item.src} />
                              ))}
                          </Box>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Typography
                  textAlign={"center"}
                  variant="body1"
                  color="#95919D"
                >
                  No data found
                </Typography>
              )}
            </Box>
          </Box>
        )}

        {tab === 2 && (
          <Box mt={3} sx={{ backgroundColor: "#F8F8FA", borderRadius: 2, p: 1.5 }}>
            <Box
              sx={{
                maxHeight: "80vh",
                overflowY: "auto",
                pr: 1,
                // Styled scrollbar - Chrome, Safari, Edge
                "&::-webkit-scrollbar": {
                  width: "6px",
                },
                "&::-webkit-scrollbar-track": {
                  backgroundColor: "#f1f1f1",
                  borderRadius: "10px",
                },
                "&::-webkit-scrollbar-thumb": {
                  backgroundColor: "#c1c1c1",
                  borderRadius: "10px",
                  "&:hover": {
                    backgroundColor: "#a1a1a1",
                  },
                },
                // Firefox
                scrollbarWidth: "thin",
                scrollbarColor: "#c1c1c1 #f1f1f1",
              }}
            >
              {posts?.length !== 0 ? (
                <Grid container spacing={1.2}>
                  {posts?.map((post) => (
                    <Grid item xs={12} sm={6} md={6} lg={4} key={post}>
                      <Card
                        elevation={0}
                        onClick={(e) => handleDetails(post?.id, e)}
                        sx={{
                          position: "relative",
                          boxShadow: "none",
                          borderRadius: "16px",
                          display: "flex",
                          flexDirection: "column",
                        }}
                      >
                        <Box sx={{ padding: "8px 12px 4px 12px" }}>
                          <Typography
                            color="#5D5D5D"
                            fontWeight={500}
                            sx={{
                              fontSize: "12px",
                              lineHeight: "1.4rem",
                              display: "-webkit-box",
                              WebkitLineClamp: post?.media?.length > 0 ? 2 : 6,
                              WebkitBoxOrient: "vertical",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              wordBreak: "break-word",
                              minHeight: post?.media?.length > 0 ? undefined : "60px",
                            }}
                          >
                            {post?.content || post?.title || ""}
                          </Typography>
                        </Box>
                        {post?.media?.length > 0 && (
                          <PostMediaViewer media={post?.media || []} />
                        )}
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            padding: "6px 12px",
                            borderTop: "1px solid #F5F5F5",
                            mt: "auto",
                          }}
                        >
                          <Box display="flex" alignItems="center" gap={0.6}>
                            <img src={share} alt="share" style={{ width: "12px", height: "12px" }} />
                            <Typography fontSize="11px" color="#5D5D5D">
                              {post?.postShareData?.shareCount || 0} shares
                            </Typography>
                          </Box>
                          <Box display="flex" gap={1} alignItems="center">
                            {[
                              { key: "hasInstagram", src: instagram1 },
                              { key: "hasLinkedin", src: linkedin1 },
                              { key: "hasFacebook", src: facebook1 },
                              { key: "hasX", src: twiter1 },
                            ]
                              .filter((item) => post?.postShareData?.[item.key])
                              .map((item, idx) => (
                                <Avatar key={idx} alt={item.key} sx={{ width: 18, height: 18 }} src={item.src} />
                              ))}
                          </Box>
                        </Box>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              ) : (
                <Typography
                  textAlign={"center"}
                  variant="body1"
                  color="#95919D"
                >
                  No data found
                </Typography>
              )}
            </Box>
          </Box>
        )}
      </Card>
      <Dialog
        open={open1}
        onClose={onClose}
        PaperProps={{
          sx: {
            borderRadius: "16px",
            padding: "20px",
            maxWidth: "420px",
            width: "100%",
          },
        }}
      >
        {/* Close Icon */}
        <IconButton
          onClick={onClose}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
          }}
        >
          <CloseIcon />
        </IconButton>
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#484848",
            fontSize: "20px",
            p: 0,
            mb: 1,
          }}
        >
          Are You Sure You Want To Log Out?
        </DialogTitle>
        <DialogContent sx={{ padding: "20px 5px" }}>
          <Typography
            sx={{
              fontSize: "16px",
              color: "#616161",
              mb: 3,
              textAlign: "start",
            }}
          >
            You will need to sign in again to access your account.
          </Typography>

          {/* Buttons */}
          <Box display="flex" justifyContent="end" gap={2}>
            <Button
              onClick={onClose}
              sx={{
                backgroundColor: "#F4F0FF",
                color: "#0D47A1",
                textTransform: "none",
                fontSize: "16px",
                fontWeight: 500,
                borderRadius: "8px",
                px: 4,
                py: 1,
                "&:hover": { backgroundColor: "#E9D5FF" },
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={onConfirm}
              sx={{
                backgroundColor: "#D93A3A",
                color: "#fff",
                textTransform: "none",
                fontSize: "16px",
                fontWeight: 500,
                borderRadius: "8px",
                px: 4,
                py: 1,
                "&:hover": { backgroundColor: "#b71c1c" },
              }}
            >
              Confirm
            </Button>
          </Box>
        </DialogContent>
      </Dialog>

      <Dialog
        open={open}
        onClose={onClose}
        PaperProps={{
          sx: {
            borderRadius: "20px",
            padding: "20px",
            maxWidth: "450px",
            width: "100%",
          },
        }}
      >
        {/* Close button */}
        <IconButton
          onClick={onClose}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
          }}
        >
          <CloseIcon />
        </IconButton>
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "24px",
            p: 0,
            mb: 1,
          }}
        >
          Edit Profile
        </DialogTitle>
        <DialogContent sx={{ padding: "20px 5px" }}>
          {/* Profile Image + Actions */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              mb: 3,
            }}
          >
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                position: "relative",
                background: "#FAFAFA",
                border: "1px dashed #E1E8F2",
                borderRadius: "8px",
                padding: "8px",
                width: "100%",
                justifyContent: "center",
              }}
            >
              <Avatar
                src={image}
                alt="Profile"
                sx={{ width: 200, height: 200, borderRadius: "0" }}
              />

              <Box
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  ml: 1,
                  gap: 1,
                }}
              >
                {/* Upload button */}
                <Box
                  onClick={() => fileInputRef.current.click()}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    padding: "13px",
                    cursor: "pointer",
                    borderRadius: "50%",
                    backgroundColor: "#0D47A1",
                    color: "#fff",
                    "&:hover": { backgroundColor: "#08306b" },
                  }}
                >
                  <img
                    src={uploadp}
                    style={{ width: "15px", height: "15px" }}
                  />
                </Box>

                {/* Delete button */}
                <IconButton
                  onClick={() => setImage("")}
                  sx={{
                    border: "1px solid #ddd",
                    color: "red",
                    "&:hover": { backgroundColor: "#ffe5e5" },
                  }}
                >
                  <DeleteIcon />
                </IconButton>
              </Box>

              <input
                type="file"
                hidden
                ref={fileInputRef}
                accept="image/*"
                onChange={(e) => {
                  const file = e.target.files[0];
                  if (file) handleFileUpload(file);
                }}
              />
            </Box>
          </Box>

          {/* Name Field */}
          <Typography
            variant="subtitle2"
            sx={{ color: "#95919D", fontWeight: 500 }}
          >
            Name
          </Typography>
          <TextField
            fullWidth
            value={name}
            onChange={(e) => setName(e.target.value)}
            variant="standard"
            defaultValue={name}
            sx={{ mt: 0.5, mb: 3 }}
          />

          {/* Phone Field */}
          <Typography
            variant="subtitle2"
            sx={{ color: "#95919D", fontWeight: 500 }}
          >
            Mobile Number
          </Typography>
          <TextField
            fullWidth
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            variant="standard"
            placeholder="Enter mobile number"
            sx={{ mt: 0.5, mb: 3 }}
          />

          {/* Buttons */}
          <Box display="flex" justifyContent="end" gap={2}>
            <Button
              disabled={updateLoading}
              sx={{
                backgroundColor: "#F4F0FF",
                color: "#3B0D7A",
                textTransform: "none",
                px: 4,
                py: 1,
                "&:hover": { backgroundColor: "#E9D5FF" },
              }}
              onClick={onClose}
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateData}
              disabled={updateLoading}
              sx={{
                backgroundColor: "#0047AB",
                textTransform: "none",
                color: "#fff",
                textTransform: "capitalize",
                px: 4,
                "&:hover": { backgroundColor: "#08306b" },
              }}
            >
              {updateLoading ? "Saving..." : "Update"}
            </Button>
          </Box>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default ProfileDetails;
