import {
  Avatar,
  Box,
  CircularProgress,
  Dialog,
  DialogContent,
  Grid,
  IconButton,
  Paper,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import React, { useEffect, useRef, useState } from "react";
import { Pie, PieChart } from "recharts";
import { ArrowBackIos, ArrowForwardIos, PlayArrow } from "@mui/icons-material";
import {
  getInsights,
  getSharedDetailsByPostId,
} from "../../../services/postService";
import noimg from "../../../assets/noimage.jpg";
import FacebookIcon from "../../../assets/facebook.png";
import InstagramIcon from "../../../assets/insta.png";
import LinkedInIcon from "../../../assets/linkedin.png";
import TwitterIcon from "../../../assets/twitter.png";
import {
  getBunnyEmbedUrl,
  getResolvedImageUrl,
  getResolvedPosterUrl,
  getResolvedVideoUrl,
  isBunnyStreamMedia,
} from "../../../shared/mediaResolver";

const PLATFORM_ICON_MAP = {
  FACEBOOK: FacebookIcon,
  INSTAGRAM: InstagramIcon,
  LINKEDIN: LinkedInIcon,
  X: TwitterIcon,
  TWITTER: TwitterIcon,
};

const getSharedPlatforms = (person) => {
  const platforms = [];
  if (person?.sharedInX) platforms.push({ name: "X", icon: TwitterIcon });
  if (person?.sharedInLinkedin) platforms.push({ name: "LinkedIn", icon: LinkedInIcon });
  if (person?.sharedInFacebook) platforms.push({ name: "Facebook", icon: FacebookIcon });
  if (person?.sharedInInstagram) platforms.push({ name: "Instagram", icon: InstagramIcon });
  return platforms;
};
const PostDetails = ({ open, onClose, type, title, postReviewData }) => {
  const [tab, setTab] = useState(0);
  const [reply, setReply] = useState(null);
  const [showAllReplies, setShowAllReplies] = useState({});
  const [sharesData, setSharesData] = useState([]);
  const [insights, setInsights] = useState(null);
  const [insightsLoading, setInsightsLoading] = useState(false);
  const mediaList = postReviewData?.post?.media || [];
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isBunnyPlayerActive, setIsBunnyPlayerActive] = useState(false);
  const videoRef = useRef(null);
  const shareCount = sharesData?.length || 0;
  const hasInsights = insights && (
    insights.totalImpressions > 0 || insights.totalEngagements > 0 ||
    insights.totalReach > 0 || insights.totalLikes > 0 ||
    insights.totalComments > 0 || insights.totalShares > 0
  );
  const hasMedia = mediaList.length > 0;
  const handleOnChange = async (e, v) => {
    setTab(v);
    if (v === 0) {
      fetchSharedData();
    } else {
      setInsightsLoading(true);
      fetchInsightsData().finally(() => setInsightsLoading(false));
    }
  };
  const fetchSharedData = async () => {
    try {
      const response = await getSharedDetailsByPostId(postReviewData?.post?.id);
      if (response?.status === true) {
        setSharesData(response?.shareUserDTOs);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchInsightsData = async () => {
    try {
      const response = await getInsights(postReviewData?.post?.id);
      if (response?.status === true) {
        setInsights(response);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  useEffect(() => {
    fetchSharedData();
    fetchInsightsData();
  }, []);

  useEffect(() => {
    setCurrentIndex(0);
    setIsPlaying(false);
    setIsBunnyPlayerActive(false);
  }, [open, postReviewData?.post?.id]);


  const handleNext = () => {
    if (mediaList.length > 0) {
      setCurrentIndex((prev) => (prev + 1) % mediaList.length);
      setIsPlaying(false);
      setIsBunnyPlayerActive(false);
    }
  };

  const handlePrev = () => {
    if (mediaList.length > 0) {
      setCurrentIndex((prev) => (prev - 1 + mediaList.length) % mediaList.length);
      setIsPlaying(false);
      setIsBunnyPlayerActive(false);
    }
  };

  const currentMedia = mediaList[currentIndex];

  const handlePlayToggle = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  const handleActivateBunnyPlayer = () => {
    setIsBunnyPlayerActive(true);
  };
  return (
    <Dialog
      elevation={0}
      open={open}
      PaperProps={{ sx: { p: 0, m: 0, borderRadius: 0 } }}
      onClose={onClose}
      fullScreen
    >
      <Box sx={{ backgroundColor: "#fff", height: "100%" }}>
        <Box
          bgcolor={"#ffffff"}
          boxShadow={"5px 4px 4px 0px rgba(236, 236, 236, 0.25)"}
          display="flex"
          borderBottom={"1px solid #CFCFCF"}
          sx={{ pl: "50px", pr: "20px", pt: 2, pb: 2 }}
          justifyContent="space-between"
          alignItems="center"
        >
          <Box display={"flex"} alignItems={"center"} gap={2}>
            <Avatar
              sx={{ width: 40, height: 40 }}
              src={postReviewData?.post?.createdBy?.profileImageUrl || ""}
            />
            <Box>
              <Typography fontSize={"16px"} color="#3B3B3B" fontWeight={500}>
                {postReviewData?.post?.createdBy?.name || ""}
              </Typography>
              <Typography fontSize={"15px"} color="#95919D" fontWeight={400}>
                {postReviewData?.post?.createdBy?.teamName || ""}
              </Typography>
            </Box>
          </Box>
          <IconButton sx={{ width: "24px", height: "24px" }} onClick={onClose}>
            <CloseIcon sx={{ color: "rgba(138, 138, 138, 1)" }} />
          </IconButton>
        </Box>

        <DialogContent sx={{ pl: 6, pr: 6, pb: 0, pt: "10px" }}>
          <Grid container spacing={2}>
            {/* Left Column — Media or Text content */}
            <Grid item xs={12} md={5.5}>
              {hasMedia ? (<>
              <Box
                sx={{
                  position: "relative",
                  borderRadius: "8px",
                  width: "100%",
                  height: "65vh",
                  overflow: "hidden",
                  backgroundColor: "#000",
                }}
              >
                {/* Media Display */}
                {currentMedia ? (
                  currentMedia?.mediaType === "IMAGE" ? (
                    <img
                      src={getResolvedImageUrl(currentMedia, noimg)}
                      alt="post"
                      style={{
                        width: "100%",
                        height: "100%",
                        objectFit: "contain",
                        borderRadius: "8px",
                      }}
                    />
                  ) : currentMedia?.mediaType === "VIDEO" ? (
                    <Box sx={{ position: "relative", height: "100%" }}>
                      {isBunnyStreamMedia(currentMedia) ? (
                        isBunnyPlayerActive ? (
                          <iframe
                            src={getBunnyEmbedUrl(
                              getResolvedVideoUrl(currentMedia)
                            )}
                            title={postReviewData?.post?.title || "Post video"}
                            style={{
                              width: "100%",
                              height: "100%",
                              border: 0,
                              display: "block",
                              borderRadius: "8px",
                              background: "#000",
                            }}
                            allow="accelerometer; gyroscope; encrypted-media; picture-in-picture;"
                            allowFullScreen
                          />
                        ) : (
                          <>
                            <img
                              src={getResolvedPosterUrl(currentMedia, noimg)}
                              alt="video poster"
                              style={{
                                width: "100%",
                                height: "100%",
                                objectFit: "contain",
                                borderRadius: "8px",
                                background: "#000",
                              }}
                            />
                            <IconButton
                              onClick={handleActivateBunnyPlayer}
                              sx={{
                                position: "absolute",
                                top: "50%",
                                left: "50%",
                                transform: "translate(-50%, -50%)",
                                color: "#fff",
                                backgroundColor: "rgba(0,0,0,0.5)",
                                borderRadius: "50%",
                                padding: "16px",
                                "&:hover": { backgroundColor: "rgba(0,0,0,0.7)" },
                              }}
                            >
                              <PlayArrow sx={{ fontSize: 48 }} />
                            </IconButton>
                          </>
                        )
                      ) : (
                        <>
                          <video
                            ref={videoRef}
                            src={getResolvedVideoUrl(currentMedia)}
                            poster={getResolvedPosterUrl(currentMedia)}
                            style={{
                              width: "100%",
                              height: "100%",
                              objectFit: "contain",
                              borderRadius: "8px",
                              background: "#000",
                            }}
                            onPlay={() => setIsPlaying(true)}
                            onPause={() => setIsPlaying(false)}
                          />
                          {!isPlaying && (
                            <IconButton
                              onClick={handlePlayToggle}
                              sx={{
                                position: "absolute",
                                top: "50%",
                                left: "50%",
                                transform: "translate(-50%, -50%)",
                                color: "#fff",
                                backgroundColor: "rgba(0,0,0,0.5)",
                                borderRadius: "50%",
                                padding: "16px",
                                "&:hover": { backgroundColor: "rgba(0,0,0,0.7)" },
                              }}
                            >
                              <PlayArrow sx={{ fontSize: 48 }} />
                            </IconButton>
                          )}
                        </>
                      )}
                    </Box>
                  ) : null
                ) : (
                  <img
                    src={noimg}
                    alt="no media"
                    style={{
                      width: "100%",
                      height: "100%",
                      objectFit: "cover",
                      borderRadius: "8px",
                    }}
                  />
                )}

                {/* Navigation Buttons */}
                {mediaList.length > 1 && (
                  <>
                    <IconButton
                      onClick={handlePrev}
                      sx={{
                        position: "absolute",
                        top: "50%",
                        left: "16px",
                        transform: "translateY(-50%)",
                        backgroundColor: "rgba(255,255,255,0.6)",
                        "&:hover": { backgroundColor: "rgba(255,255,255,0.8)" },
                      }}
                    >
                      <ArrowBackIos />
                    </IconButton>

                    <IconButton
                      onClick={handleNext}
                      sx={{
                        position: "absolute",
                        top: "50%",
                        right: "16px",
                        transform: "translateY(-50%)",
                        backgroundColor: "rgba(255,255,255,0.6)",
                        "&:hover": { backgroundColor: "rgba(255,255,255,0.8)" },
                      }}
                    >
                      <ArrowForwardIos />
                    </IconButton>
                  </>
                )}
              </Box>

              {/* Text Content BELOW - with scroll */}
              <Box
                sx={{
                  mt: 2,
                  maxHeight: "15vh",
                  overflowY: "auto",
                  scrollbarWidth: "thin",
                  "&::-webkit-scrollbar": {
                    width: "4px",
                  },
                  "&::-webkit-scrollbar-thumb": {
                    backgroundColor: "#ccc",
                    borderRadius: "4px",
                  },
                }}
              >
                <Typography
                  variant="body1"
                  fontSize="14px"
                  color="#3B3B3B"
                  fontWeight={400}
                  lineHeight={1.6}
                >
                  {postReviewData?.post?.content}
                </Typography>
              </Box>
            </>) : (
              <Box
                sx={{
                  borderRadius: "8px",
                  width: "100%",
                  height: "65vh",
                  overflow: "auto",
                  backgroundColor: "#F8F8FA",
                  padding: 3,
                  scrollbarWidth: "thin",
                }}
              >
                <Typography
                  variant="body1"
                  fontSize="15px"
                  color="#3B3B3B"
                  fontWeight={400}
                  lineHeight={1.8}
                  whiteSpace="pre-wrap"
                >
                  {postReviewData?.post?.content}
                </Typography>
              </Box>
            )}
            </Grid>

            <Grid item xs={12} md={6.5}>
              <Box mb={2} mt={2}>
                <Tabs
                  value={tab}
                  // onChange={(e, v) => setTab(v)}
                  onChange={(e, v) => handleOnChange(e, v)}
                  variant="fullWidth"
                  TabIndicatorProps={{ style: { display: "none" } }}
                  sx={{
                    backgroundColor: "#F5F5F9",
                    borderRadius: 10,
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
                    },
                    "& .Mui-selected": {
                      color: "#3B3B3B !important",
                      background: "#fff",
                      borderRadius: "20px",
                      fontWeight: 600,
                    },
                  }}
                >
                  {/* <Tab
                    label={
                      <Box display={"flex"} alignItems={"center"} gap={1}>
                        <img src={heart} height={20} width={20} alt="heart" />
                        <div>
                          Likes&nbsp;(
                          <span style={{ color: "#2D76DC" }}>{likeCount}</span>)
                        </div>
                      </Box>
                    }
                  /> */}
                  {/* <Tab
                    label={
                      <Box>
                        Comments&nbsp; (
                        <span style={{ color: "#2D76DC" }}>{commentCount}</span>
                        )
                      </Box>
                    }
                  /> */}
                  <Tab
                    label={
                      <Box>
                        Shares&nbsp; (
                        <span style={{ color: "#2D76DC" }}>{shareCount}</span>)
                      </Box>
                    }
                  />
                  {hasInsights && (
                    <Tab
                      label={
                        <Box>
                          Insights
                        </Box>
                      }
                    />
                  )}
                </Tabs>
              </Box>
              <Box
                sx={{
                  overflowY: "auto",
                  maxHeight: "calc(100vh - 200px)",
                  pr: 1,
                  scrollbarWidth: "none",
                  "&::-webkit-scrollbar": {
                    display: "none",
                  },
                }}
              >
                {/* {tab === 0 && (
                  <>
                    {people.map((person, i) => (
                      <Box
                        key={i}
                        display="flex"
                        alignItems="center"
                        bgcolor="#F5F5F9"
                        p={1.5}
                        mb={1}
                        borderRadius={3}
                      >
                        <Avatar
                          src={person.avatar}
                          sx={{ width: 40, height: 40, mr: 2 }}
                        />
                        <Box>
                          <Typography
                            fontWeight={500}
                            fontSize={"16px"}
                            color="#3B3B3B"
                          >
                            {person.name}
                          </Typography>
                          <Typography
                            fontSize={"12px"}
                            fontWeight={400}
                            color="#95919D"
                          >
                            {person.role}
                          </Typography>
                        </Box>
                      </Box>
                    ))}
                  </>
                )}
                {tab === 1 && (
                  <>
                    <Box padding={"10px"} mb={2}>
                      <CommentBox />
                    </Box>
                    {people.map((person, i) => (
                      <React.Fragment key={i}>
                        <Box
                          key={i}
                          bgcolor="#F5F5F9"
                          p={1.5}
                          mb={1}
                          borderRadius={3}
                        >
                          <Box display="flex" alignItems="center">
                            <Avatar
                              src={person.avatar}
                              sx={{ width: 40, height: 40, mr: 2 }}
                            />
                            <Box>
                              <Typography
                                fontWeight={400}
                                fontSize={"16px"}
                                color="#3B3B3B"
                              >
                                {person.name}
                              </Typography>
                              <Typography
                                fontSize={"12px"}
                                fontWeight={400}
                                color="#95919D"
                              >
                                {person.role}
                              </Typography>
                            </Box>
                          </Box>
                          <Box
                            p={1}
                            mt={1}
                            display={"flex"}
                            justifyContent={"space-between"}
                          >
                            <Typography fontSize={"16px"} color="#3B3B3B">
                              wOW great Idea!! Stay ahead in the digital race!
                            </Typography>
                            <Box display={"flex"} gap={2}>
                              <Box
                                display={"flex"}
                                alignItems={"center"}
                                gap={1}
                              >
                                <img
                                  src={like}
                                  alt="like"
                                  width={8}
                                  height={11}
                                />
                                <Typography fontSize={"12px"} color="#3B3B3B">
                                  12
                                </Typography>
                              </Box>
                              <Box
                                display={"flex"}
                                alignItems={"center"}
                                gap={1}
                              >
                                <Typography
                                  fontSize={"12px"}
                                  fontWeight={500}
                                  sx={{
                                    cursor: "pointer",
                                    transition: "color 0.2s",
                                    "&:hover": {
                                      color: "#2D76DC", // Change this to your desired hover color
                                    },
                                  }}
                                  color="#8296B2"
                                  onClick={() => setReply(i)}
                                >
                                  Reply
                                </Typography>
                                <Typography fontSize={"12px"} color="#3B3B3B">
                                  10
                                </Typography>
                              </Box>
                            </Box>
                          </Box>
                        </Box>

                        {reply === i && (
                          <Box
                            padding="10px"
                            mt={2}
                            ml={1}
                            pl={4.5}
                            borderLeft={"1px solid #D7D4D4"}
                            mb={2}
                          >
                            <CommentBox />
                            {(showAllReplies[i]
                              ? comments
                              : comments.slice(0, 3)
                            ).map((person, idx) => (
                              <React.Fragment key={idx}>
                                <Box
                                  mt={2}
                                  bgcolor="#F5F5F9"
                                  p={1.5}
                                  mb={1}
                                  borderRadius={3}
                                >
                                  <Box display="flex" alignItems="center">
                                    <Avatar
                                      src={person.avatar}
                                      sx={{ width: 40, height: 40, mr: 2 }}
                                    />
                                    <Box>
                                      <Typography
                                        fontWeight={500}
                                        fontSize="16px"
                                        color="#3B3B3B"
                                      >
                                        {person.name}
                                      </Typography>
                                      <Typography
                                        fontSize="12px"
                                        fontWeight={400}
                                        color="#95919D"
                                      >
                                        {person.role}
                                      </Typography>
                                    </Box>
                                  </Box>
                                  <Box
                                    p={1}
                                    mt={1}
                                    display={"flex"}
                                    justifyContent={"space-between"}
                                  >
                                    <Typography
                                      fontSize={"16px"}
                                      color="#3B3B3B"
                                    >
                                      @Manu George Interesting!!!
                                    </Typography>
                                    <Box display={"flex"} gap={2}>
                                      <Box
                                        display={"flex"}
                                        alignItems={"center"}
                                        gap={1}
                                      >
                                        <img
                                          src={like}
                                          alt="like"
                                          width={8}
                                          height={11}
                                        />
                                        <Typography
                                          fontSize={"12px"}
                                          color="#3B3B3B"
                                        >
                                          12
                                        </Typography>
                                      </Box>
                                    </Box>
                                  </Box>
                                </Box>
                              </React.Fragment>
                            ))}

                            {!showAllReplies[i] && comments.length > 3 && (
                              <Typography
                                onClick={() =>
                                  setShowAllReplies((prev) => ({
                                    ...prev,
                                    [i]: true,
                                  }))
                                }
                                fontSize="18px"
                                mt={2}
                                mb={2}
                                color="#2D76DC"
                                fontWeight={600}
                                sx={{
                                  color: "#2D76DC",
                                  cursor: "pointer",
                                  textAlign: "center",
                                  "&:hover": { textDecoration: "underline" },
                                }}
                              >
                                Load More Comments
                              </Typography>
                            )}
                          </Box>
                        )}
                      </React.Fragment>
                    ))}
                  </>
                )} */}
                {tab === 0 && (
                  <>
                    {sharesData?.length > 0 ? (
                      sharesData?.map((person, i) => {
                        const sharedTo = getSharedPlatforms(person);
                        const sourceIcon = PLATFORM_ICON_MAP[postReviewData?.post?.type?.toUpperCase()];
                        return (
                        <Box
                          key={i}
                          bgcolor="#F5F5F9"
                          p={2}
                          mb={1}
                          borderRadius={3}
                        >
                          <Box display="flex" alignItems="center" justifyContent="space-between">
                            <Box display="flex" alignItems="center">
                              <Avatar
                                src={person?.profilePicture}
                                sx={{ width: 40, height: 40, mr: 2 }}
                              />
                              <Box>
                                <Typography
                                  fontWeight={500}
                                  fontSize={"15px"}
                                  color="#3B3B3B"
                                >
                                  {person?.userName}
                                </Typography>
                                <Typography
                                  fontSize={"12px"}
                                  fontWeight={400}
                                  color="#95919D"
                                >
                                  {person.jobTitle}
                                </Typography>
                              </Box>
                            </Box>
                          </Box>
                          {sharedTo.length > 0 && (
                            <Box display="flex" alignItems="center" mt={1.5} ml={7} gap={0.5}>
                              <Typography fontSize="12px" color="#95919D" fontWeight={400}>
                                Shared to
                              </Typography>
                              {sharedTo.map((platform, j) => (
                                <Box key={j} display="flex" alignItems="center" gap={0.3}>
                                  <Avatar
                                    src={platform.icon}
                                    sx={{ width: 18, height: 18 }}
                                    alt={platform.name}
                                  />
                                  <Typography fontSize="12px" color="#3B3B3B" fontWeight={400}>
                                    {platform.name}{j < sharedTo.length - 1 ? "," : ""}
                                  </Typography>
                                </Box>
                              ))}
                            </Box>
                          )}
                        </Box>
                        );
                      })
                    ) : (
                      <Typography textAlign={"center"} color="#95919D" mt={4}>
                        No shares data found.
                      </Typography>
                    )}
                  </>
                )}
                {tab === 1 && (
                  <Box sx={{ position: "relative", minHeight: 150 }}>
                    {insightsLoading && (
                      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 6 }}>
                        <CircularProgress size={32} />
                        <Typography sx={{ ml: 2, color: "#6B7280", fontSize: 13 }}>Loading insights...</Typography>
                      </Box>
                    )}
                    {!insightsLoading && <>
                    {/* Top Metric Cards */}
                    <Grid container spacing={1.5} mb={2}>
                      {[
                        {
                          label: "Total Impressions",
                          value: insights?.totalImpressions || 0,
                          desc: "Total views across all platforms",
                        },
                        {
                          label: "Total Engagements",
                          value: insights?.totalEngagements || 0,
                          desc: "Likes, comments, shares combined",
                        },
                        {
                          label: "Total Reach",
                          value: insights?.totalReach || 0,
                          desc: "Unique users who saw this post",
                        },
                        {
                          label: "Click-Through Rate",
                          value: insights?.clickThroughRate || 0,
                          desc: "Percentage of users who clicked",
                          isPercentage: true,
                        },
                      ].filter((item) => item.value > 0 || item.label === "Total Impressions").map((item, index) => (
                        <Grid item xs={12} sm={6} md={3} key={index}>
                          <Box
                            p={2}
                            borderRadius={2}
                            bgcolor="#F5F5F9"
                            textAlign="center"
                          >
                            <Typography
                              fontSize="11px"
                              fontWeight={400}
                              color="#3B3B3B"
                            >
                              {item.label}
                            </Typography>

                            <Typography mt={1} fontSize="9px" color="#989898">
                              {item.desc}
                            </Typography>
                            <Typography
                              fontSize="20px"
                              fontWeight={600}
                              mt={1}
                              color="#2D76DC"
                            >
                              {item.value}
                            </Typography>
                          </Box>
                        </Grid>
                      ))}
                    </Grid>

                    {/* Bottom Donut Charts */}
                    {/* <Grid container spacing={2}>
                      {[
                        { title: "Impressions" },
                        { title: "Engagements" },
                        { title: "Reaches" },
                        { title: "Click- Through Rate" },
                      ].map((title, i) => (
                        <Grid item xs={12} sm={6} md={6}>
                          <Box p={3} borderRadius={2} bgcolor="#FAFAFC">
                            <Typography
                              fontWeight={500}
                              fontSize="12px"
                              color="#3B3B3B"
                              mb={1}
                            >
                              {title?.title}
                            </Typography>

                            <Box
                              display="flex"
                              alignItems="center"
                              justifyContent="space-between"
                            >
                              <PieChart width={150} height={150}>
                                <Pie
                                  dataKey="value"
                                  data={[
                                    {
                                      name: "LinkedIn",
                                      value: 41,
                                      fill: "#0065C0",
                                    },
                                    {
                                      name: "Facebook",
                                      value: 16,
                                      fill: "#5C76B7",
                                    },
                                    {
                                      name: "Twitter",
                                      value: 16,
                                      fill: "#000000",
                                    },
                                    {
                                      name: "Instagram",
                                      value: 10,
                                      fill: "#D93CC8",
                                    },
                                    {
                                      name: "Internally",
                                      value: 16,
                                      fill: "#E4E9F0",
                                    },
                                  ]}
                                  innerRadius={55}
                                  outerRadius={70}
                                  stroke="none"
                                />
                                <text
                                  x={80}
                                  y={70}
                                  textAnchor="middle"
                                  dominantBaseline="middle"
                                  fontSize="12"
                                  fill="#9B9B9B"
                                >
                                  Total
                                </text>
                                <text
                                  x={80}
                                  y={90}
                                  textAnchor="middle"
                                  dominantBaseline="middle"
                                  fontSize="18"
                                  fontWeight="bold"
                                  fill="#3B3B3B"
                                >
                                  22,666
                                </text>
                              </PieChart>

                        
                              <Box>
                                {[
                                  {
                                    label: "Linkden",
                                    value: "41%",
                                    color: "#0065C0",
                                  },
                                  {
                                    label: "Facebook",
                                    value: "16%",
                                    color: "#5C76B7",
                                  },
                                  {
                                    label: "Twitter",
                                    value: "16%",
                                    color: "#000000",
                                  },
                                  {
                                    label: "Instagram",
                                    value: "10%",
                                    color: "#D93CC8",
                                  },
                                  {
                                    label: "Internally",
                                    value: "16%",
                                    color: "#E4E9F0",
                                  },
                                ].map((item, idx) => (
                                  <Box
                                    key={idx}
                                    display="flex"
                                    alignItems="center"
                                    mb={1}
                                  >
                                    <Box
                                      width={10}
                                      height={10}
                                      borderRadius="50%"
                                      bgcolor={item.color}
                                      mr={1.5}
                                    />
                                    <Typography fontSize="11px" color="#5D5D5D">
                                      {item.label} {item.value}
                                    </Typography>
                                  </Box>
                                ))}
                              </Box>
                            </Box>
                          </Box>
                        </Grid>
                      ))}
                    </Grid> */}

                    <Typography
                      fontSize="11px"
                      color="#95919D"
                      mb={1.5}
                    >
                      {insights?.devMessage || "Analytics data shown is based on available platform APIs. Some platforms may have limited metrics."}
                    </Typography>

                    <Grid container spacing={2}>
                      {[
                        {
                          title: "Impressions",
                          key: "platformImpression",
                          totalKey: "totalImpressions",
                        },
                        {
                          title: "Estimated Engagements",
                          key: "platformEngagement",
                          totalKey: "totalEngagements",
                        },
                        {
                          title: "Reach",
                          key: "platformReach",
                          totalKey: "totalReach",
                        },
                        {
                          title: "Click-Through Rate",
                          key: "platformClickThroughRate",
                          totalKey: "clickThroughRate",
                          isPercentage: true,
                        },
                      ].filter((metric) => {
                        const platformData = insights?.[metric.key] || {};
                        const total = Object.values(platformData).reduce((sum, v) => sum + (v || 0), 0);
                        return total > 0;
                      }).map((metric, i) => {
                        const platformData = insights?.[metric.key] || {};
                        const totalValue = insights?.[metric.totalKey] ?? 0;

                        const chartData = [
                          {
                            name: "LinkedIn",
                            value: platformData.LINKEDIN || 0,
                            fill: "#0065C0",
                          },
                          {
                            name: "Facebook",
                            value: platformData.FACEBOOK || 0,
                            fill: "#5C76B7",
                          },
                          {
                            name: "Twitter",
                            value: platformData.X || 0,
                            fill: "#000000",
                          },
                          {
                            name: "Instagram",
                            value: platformData.INSTAGRAM || 0,
                            fill: "#D93CC8",
                          },
                        ];

                        const totalSum = chartData.reduce(
                          (sum, d) => sum + d.value,
                          0
                        );
                        const chartWithPercentages = chartData.map((d) => ({
                          ...d,
                          percent:
                            totalSum > 0
                              ? ((d.value / totalSum) * 100).toFixed(1) + "%"
                              : "0%",
                        }));

                        return (
                          <Grid key={i} item xs={12} sm={6} md={6}>
                            <Box p={3} borderRadius={2} bgcolor="#FAFAFC">
                              <Typography
                                fontWeight={500}
                                fontSize="12px"
                                color="#3B3B3B"
                                mb={1}
                              >
                                {metric.title}
                              </Typography>

                              <Box
                                display="flex"
                                alignItems="center"
                                justifyContent="space-between"
                              >
                                {/* Donut Chart */}
                                <PieChart width={150} height={150}>
                                  <Pie
                                    dataKey="value"
                                    data={chartData}
                                    innerRadius={55}
                                    outerRadius={70}
                                    stroke="none"
                                  />
                                  <text
                                    x={80}
                                    y={70}
                                    textAnchor="middle"
                                    dominantBaseline="middle"
                                    fontSize="12"
                                    fill="#9B9B9B"
                                  >
                                    Total
                                  </text>
                                  <text
                                    x={80}
                                    y={90}
                                    textAnchor="middle"
                                    dominantBaseline="middle"
                                    fontSize="18"
                                    fontWeight="bold"
                                    fill="#3B3B3B"
                                  >
                                    {metric.isPercentage
                                      ? `${totalValue?.toFixed?.(2) || 0}%`
                                      : totalValue}
                                  </text>
                                </PieChart>

                                {/* Legend */}
                                <Box>
                                  {chartWithPercentages.map((item, idx) => (
                                    <Box
                                      key={idx}
                                      display="flex"
                                      alignItems="center"
                                      mb={1}
                                    >
                                      <Box
                                        width={10}
                                        height={10}
                                        borderRadius="50%"
                                        bgcolor={item.fill}
                                        mr={1.5}
                                      />
                                      <Typography
                                        fontSize="11px"
                                        color="#5D5D5D"
                                      >
                                        {item.name} {item.percent}
                                      </Typography>
                                    </Box>
                                  ))}
                                </Box>
                              </Box>
                            </Box>
                          </Grid>
                        );
                      })}
                    </Grid>

                    {/* Engagement Breakdown Table */}
                    {(() => {
                      const metrics = [
                        { label: "Likes", key: "platformLikes", totalKey: "totalLikes" },
                        { label: "Comments", key: "platformComments", totalKey: "totalComments" },
                        { label: "Shares", key: "platformShares", totalKey: "totalShares" },
                        { label: "Saves", key: "platformSaves", totalKey: "totalSaves" },
                        { label: "Bookmarks", key: "platformBookmarks", totalKey: "totalBookmarks" },
                        { label: "Retweets", key: "platformRetweets" },
                        { label: "Replies", key: "platformReplies" },
                        { label: "Quote Tweets", key: "platformQuotes" },
                        { label: "Video Views", key: "platformVideoViews", totalKey: "totalVideoViews" },
                        { label: "Clicks", key: "platformClickCount" },
                      ];
                      const platforms = [
                        { key: "FACEBOOK", label: "Facebook", color: "#5C76B7" },
                        { key: "INSTAGRAM", label: "Instagram", color: "#D93CC8" },
                        { key: "X", label: "X", color: "#000000" },
                        { key: "LINKEDIN", label: "LinkedIn", color: "#0065C0" },
                      ];
                      const activePlatforms = platforms.filter(p =>
                        metrics.some(m => (insights?.[m.key]?.[p.key] || 0) > 0)
                      );
                      const activeMetrics = metrics.filter(m =>
                        platforms.some(p => (insights?.[m.key]?.[p.key] || 0) > 0)
                      );
                      if (activeMetrics.length === 0 || activePlatforms.length === 0) return null;
                      return (
                        <Box mt={2} p={2} borderRadius={2} bgcolor="#FAFAFC">
                          <Typography fontWeight={500} fontSize="13px" color="#3B3B3B" mb={1.5}>
                            Engagement Breakdown
                          </Typography>
                          <Box sx={{ overflowX: "auto" }}>
                            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "12px" }}>
                              <thead>
                                <tr style={{ borderBottom: "1px solid #E2E8F0" }}>
                                  <th style={{ textAlign: "left", padding: "6px 8px", color: "#6B7280", fontWeight: 500 }}>Metric</th>
                                  {activePlatforms.map(p => (
                                    <th key={p.key} style={{ textAlign: "center", padding: "6px 8px", color: p.color, fontWeight: 600 }}>{p.label}</th>
                                  ))}
                                  <th style={{ textAlign: "center", padding: "6px 8px", color: "#3B3B3B", fontWeight: 600 }}>Total</th>
                                </tr>
                              </thead>
                              <tbody>
                                {activeMetrics.map((m, i) => {
                                  const total = activePlatforms.reduce((sum, p) => sum + (insights?.[m.key]?.[p.key] || 0), 0);
                                  return (
                                    <tr key={m.label} style={{ borderBottom: i < activeMetrics.length - 1 ? "1px solid #F1F5F9" : "none" }}>
                                      <td style={{ padding: "8px", color: "#475569", fontWeight: 500 }}>{m.label}</td>
                                      {activePlatforms.map(p => {
                                        const val = insights?.[m.key]?.[p.key] || 0;
                                        return (
                                          <td key={p.key} style={{ textAlign: "center", padding: "8px", color: val > 0 ? "#3B3B3B" : "#CBD5E1" }}>
                                            {val > 0 ? val.toLocaleString() : "—"}
                                          </td>
                                        );
                                      })}
                                      <td style={{ textAlign: "center", padding: "8px", fontWeight: 600, color: "#2D76DC" }}>
                                        {total > 0 ? total.toLocaleString() : "—"}
                                      </td>
                                    </tr>
                                  );
                                })}
                              </tbody>
                            </table>
                          </Box>
                        </Box>
                      );
                    })()}
                    </>}
                  </Box>
                )}
              </Box>
            </Grid>
          </Grid>
        </DialogContent>
      </Box>
    </Dialog>
  );
};

export default PostDetails;
