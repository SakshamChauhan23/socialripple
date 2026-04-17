import React, { useEffect, useState } from "react";
import {
  Card,
  CardMedia,
  Typography,
  Avatar,
  Box,
  Grid,
  Tabs,
  Tab,
} from "@mui/material";
import "./Teamdetails.css";
import facebook from "../../../../assets/socialmedia/facebook.svg";
import insta from "../../../../assets/socialmedia/insta.svg";
import linkedin from "../../../../assets/socialmedia/linkedin.svg";
import X from "../../../../assets/socialmedia/twiiter.svg";
import coins from "../../../../assets/socialmedia/coins.svg";
import noImage from "../../../../assets/noimage.jpg";
import share from "../../../../assets/profile/share.png";
import instagram1 from "../../../../assets/insta.png";
import linkedin1 from "../../../../assets/linkedin.png";
import facebook1 from "../../../../assets/facebook.png";
import twiter1 from "../../../../assets/twitter.png";
import { useParams } from "react-router-dom";
import UserService from "../../../../services/categoryService";
import PostDetails from "../../Postcards/PostDetails";
import { postDetailsByPostId } from "../../../../services/postService";
import PostMediaViewer from "./mediaListing";
const TeamDetails = () => {
  const { id } = useParams(); // <-- gets "5"
  const [tab, setTab] = useState(0);
  const [profileData, setProfileData] = useState(null);
  const [platformCount, setPlatformCount] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loyaltyPoints, setLoyaltyPoints] = useState(0);
  const [postsCount, setPostsCount] = useState(0);
  const [sharesCount, setSharesCount] = useState(0);
  const [tagsCount, setTagsCount] = useState(0);
  const [postDetails, setPostDetails] = useState(false);
  const [postReviewData, setPostReviewData] = useState(null);
  const fetchProfile = async () => {
    try {
      const response = await UserService?.getProfile(id);
      if (response?.status === true) {
        setProfileData(response?.data);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchCounting = async () => {
    try {
      const response = await UserService.getCounting(id);
      if (response?.status === true) {
        setPlatformCount(response?.platformCountMap);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchLoyaltyPoints = async () => {
    try {
      const response = await UserService.getLoyaltyPoints(id);
      if (response?.status === true) {
        setLoyaltyPoints(response?.loyalty?.loyaltyPoints);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };

  const fetchSharesCount = async () => {
    try {
      const response = await UserService.getShares(id);
      if (response?.status === true) {
        setSharesCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {}
  };
  const fetchTagsCount = async () => {
    try {
      const response = await UserService.getTags(id);
      if (response?.status === true) {
        setTagsCount(response?.totalElements ?? response?.posts?.length ?? 0);
      }
    } catch (error) {}
  };

  useEffect(() => {
    fetchProfile();
    fetchLoyaltyPoints();
    fetchCounting();
    fetchActivities();
    fetchSharesCount();
    fetchTagsCount();
  }, [id]);

  const fetchActivities = async () => {
    try {
      const response = await UserService.getActivities(id);
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
      const response = await UserService.getShares(id);
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
      const response = await UserService.getTags(id);
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
  const onClose = () => {
    setPostDetails(false);
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
    }
  };
  return (
    <>
      {postReviewData && postDetails && (
        <PostDetails
          open={postDetails}
          postReviewData={postReviewData}
          onClose={onClose}
        />
      )}
      <Card
        sx={{ height: "100%" }}
        elevation={0}
        className="teamsDeatail-container"
      >
        {/* Profile Section */}
        <Box className="team-member-profile">
          {/* Left - Avatar & Name */}
          <Box display="flex" alignItems="center">
            <Avatar
              sx={{
                width: 100,
                height: 100,
                bgcolor: "grey.300",
                border: "4px solid white",
              }}
              src={profileData?.profilePicture || ""}
            />
            <Box ml={2}>
              <Typography
                variant="h6"
                color="#FFFFFF"
                fontSize={"26px"}
                fontWeight="600"
              >
                {profileData?.name || ""}
              </Typography>
              <Typography
                variant="body2"
                fontWeight="400"
                fontSize={"16px"}
                color="#E7E7E7"
              >
                {[profileData?.jobTitle, profileData?.teamName].filter(Boolean).join(" | ")}
              </Typography>
            </Box>
          </Box>

          {/* Right - Coin count & Social Media Stats */}
          <Box display="flex" flexDirection="column" alignItems="flex-end">
            {/* Coin Count at the Top Right */}
            <Box display="flex" alignItems="center" mb={2}>
              <img src={coins} alt="facebook" width={23} height={23} />
              <Typography
                variant="h6"
                fontSize={"20px"}
                fontWeight="500"
                ml={1}
              >
                {loyaltyPoints || 0}
              </Typography>
            </Box>

            {/* Social Media Stats Below */}
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
          <Box mb={3} mt={2} width={"53%"}>
            <Tabs
              value={tab}
              onChange={(e, v) => setupTabs(v)}
              variant="fullWidth"
              TabIndicatorProps={{ style: { display: "none" } }}
              sx={{
                backgroundColor: "#F5F5F9",
                borderRadius: "50px",
                pt: 0.6,
                pb: 0.6,
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
                sx={{
                  "&.MuiButtonBase-root": {
                    padding: "3px 6px",
                    height: "41px",
                  },
                }}
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
                sx={{
                  "&.MuiButtonBase-root": {
                    padding: "3px 6px",
                    height: "41px",
                  },
                }}
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
                sx={{
                  "&.MuiButtonBase-root": {
                    padding: "3px 6px",
                    height: "41px",
                  },
                }}
                label={
                  <Box>
                    Tags&nbsp; (
                    <span style={{ color: "#2D76DC" }}>
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

                scrollbarWidth: "none", // Firefox
                "&::-webkit-scrollbar": { display: "none" }, // Chrome, Safari
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
                maxHeight: "80vh", // or any height you need
                overflowY: "auto",
                pr: 1,

                scrollbarWidth: "none", // Firefox
                "&::-webkit-scrollbar": { display: "none" }, // Chrome, Safari
              }}
            >
              {posts?.length !== 0 ? (
                <Grid container spacing={1.2}>
                  {posts?.map((post) => (
                    <Grid item xs={12} sm={6} md={6} lg={4} key={post}>
                      <Card
                        elevation={0}
                        sx={{
                          position: "relative",
                          boxShadow: "none",
                          borderRadius: "16px",
                          display: "flex",
                          flexDirection: "column",
                        }}
                        onClick={(e) => handleDetails(post?.id, e)}
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
                        {/* <CardMedia
                          component="img"
                          height="250"
                          image={post?.media[0]?.fileUrl || noImage}
                          alt="Post Image"
                          sx={{ objectFit: "cover", cursor: "pointer" }}
                        /> */}
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
                maxHeight: "80vh", // or any height you need
                overflowY: "auto",
                pr: 1,

                scrollbarWidth: "none", // Firefox
                "&::-webkit-scrollbar": { display: "none" }, // Chrome, Safari
              }}
            >
              {posts?.length !== 0 ? (
                <Grid container spacing={1.2}>
                  {posts?.map((post) => (
                    <Grid item xs={12} sm={6} md={6} lg={4} key={post}>
                      <Card
                        elevation={0}
                        sx={{
                          position: "relative",
                          boxShadow: "none",
                          borderRadius: "16px",
                          display: "flex",
                          flexDirection: "column",
                        }}
                        onClick={(e) => handleDetails(post?.id, e)}
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
                        {/* <CardMedia
                          component="img"
                          height="250"
                          image={post?.media[0]?.fileUrl || noImage}
                          alt="Post Image"
                          sx={{ objectFit: "cover", cursor: "pointer" }}
                        /> */}
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
    </>
  );
};

export default TeamDetails;
