import {
  Box,
  Button,
  Dialog,
  DialogContent,
  IconButton,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import React, { useEffect, useState } from "react";
import insta from "../../assets/insta.png";
import fb from "../../assets/facebook.png";
import twit from "../../assets/twitter.png";
import linkedin from "../../assets/linkedin.png";
import star from "../../assets/points.svg";
import noimg from "../../assets/noimage.jpg";
import img1 from "../../assets/timeline/pic1.png";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import UserService from "../../services/categoryService";
import { postDetailsByPostId } from "../../services/postService";
import PostDetails from "../Layout/Postcards/PostDetails";
import MediaCarousel from "./mediaCarosel";
const ActivitiesPopup = ({ open, onClose }) => {
  const [activeTab, setActiveTab] = useState("Shares");
  const [data, setData] = useState([]);
  const [postDetails, setPostDetails] = useState(false);
  const [postReviewData, setPostReviewData] = useState(null);
  const tabs = [
    "Shares",
    "Posts",
    //  "Likes & Comments"
  ];

  const fetchAllData = async () => {
    try {
      if (activeTab === "Shares") {
        const response = await UserService.getUserActivitiesShares();
        if (response?.status === true) {
          setData(response?.posts);
        }
      } else {
        const response = await UserService.getUserActivitiesPosts();
        if (response?.status === true) {
          setData(response?.posts);
        }
      }
    } catch (error) {
      console.log(error);
    } finally {
    }
  };
  useEffect(() => {
    if (open) {
      fetchAllData();
    }
  }, [activeTab, open]);

  const handleDetails = async (id) => {
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
    }
  };

  const onPostDetails = () => {
    setPostDetails(false);
    setPostReviewData(null);
  };

  return (
    <>
      {postReviewData && postDetails && (
        <PostDetails
          open={postDetails}
          postReviewData={postReviewData}
          onClose={onPostDetails}
        />
      )}

      <Dialog
        elevation={0}
        open={open}
        PaperProps={{ sx: { p: 0, m: 0, borderRadius: 0 } }}
        onClose={onClose}
        fullScreen
      >
        <Box sx={{ backgroundColor: "#f6f3ff", height: "100%" }}>
          <Box
            bgcolor={"#ffffff"}
            boxShadow={"0px 4px 20px rgba(236, 236, 236, 0.25)"}
            display="flex"
            sx={{ pl: "50px", pr: "20px", pt: 2, pb: 2 }}
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography
              variant="h6"
              color="rgba(59, 59, 59, 1)"
              fontWeight={500}
            >
              Activities
            </Typography>
            <IconButton
              sx={{ width: "24px", height: "24px" }}
              onClick={onClose}
            >
              <CloseIcon sx={{ color: "rgba(138, 138, 138, 1)" }} />
            </IconButton>
          </Box>

          <DialogContent sx={{ pl: 6, pr: 6 }}>
            <Box
              sx={{
                display: "flex",
                gap: 1,
                bgcolor: "#f6f3ff",
                p: 0.5,
                borderRadius: "30px",
                mx: "auto",
                display: "flex",
                alignItems: "center",
                mb: 2,
                justifyContent: "flex-start",
              }}
            >
              <Box
                width={"fit-content"}
                bgcolor={"#F5F5F9"}
                borderRadius={"50px"}
                py={1}
                px={2}
              >
                {tabs?.map((tab) => (
                  <Button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    disableRipple
                    sx={{
                      textTransform: "none",
                      px: 4,
                      py: 0.6,
                      fontWeight: activeTab === tab ? 600 : 400,
                      fontSize: "16px",
                      borderRadius: "20px",
                      color: activeTab === tab ? "#0047AB" : "#3B3B3B",
                      backgroundColor:
                        activeTab === tab ? "#fff" : "transparent",

                      "&:hover": {
                        backgroundColor: activeTab === tab ? "#fff" : "#f0eefc",
                      },
                    }}
                  >
                    {tab}
                  </Button>
                ))}
              </Box>
            </Box>
            <Box sx={{ overflowY: "auto", height: "80vh" }}>
              {activeTab === "Shares" && (
                <>
                  {data?.length > 0 ? (
                    data?.map((_, index) => (
                      <Box
                        key={index}
                        sx={{
                          display: "flex",
                          gap: 2,
                          bgcolor: "#fff",
                          p: 2,
                          mb: 2,
                          borderRadius: "12px",
                          boxShadow: "0 1px 6px rgba(0,0,0,0.05)",
                          alignItems: "flex-start",
                          justifyContent: "space-between",
                          cursor: "pointer",
                        }}
                        onClick={(e) => handleDetails(_?.id)}
                      >
                        {/* Left: Image */}
                        {_?.media?.length > 0 ? (
                          <MediaCarousel media={_?.media || []} />
                        ) : (
                          <Box
                            sx={{
                              minWidth: "80px",
                              maxWidth: "80px",
                              height: "80px",
                              borderRadius: "8px",
                              overflow: "hidden",
                            }}
                          >
                            <img
                              src={noimg}
                              alt="post"
                              style={{
                                width: "100%",
                                height: "100%",
                                objectFit: "cover",
                              }}
                            />
                          </Box>
                        )}

                        {/* Middle: Texts */}
                        <Box sx={{ flex: 1 }}>
                          <Typography
                            variant="subtitle1"
                            sx={{
                              fontWeight: 400,
                              color: "#3B3B3B",
                              mb: 0.5,
                              fontSize: "16px",
                            }}
                          >
                            {_?.content || ""}
                          </Typography>

                          <Box
                            display="flex"
                            gap={1}
                            alignItems="center"
                            mt={1}
                          >
                            <img src={star} width={19} height={19} />
                            <Typography
                              fontSize={16}
                              fontWeight={400}
                              color="#3B3B3B"
                            >
                              {_?.earnedLoyaltyPoint || 0}
                            </Typography>
                          </Box>
                          <Typography fontSize={12} color="#2D76DC">
                            {_?.createdBy?.name || ""}
                          </Typography>
                        </Box>

                        {/* Right: Platforms & Arrow */}
                        <Box
                          sx={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 1,
                          }}
                        >
                          <Box justifyContent={"end"} display={"flex"}>
                            <NavigateNextIcon
                              sx={{ color: "#2D76DC", fontSize: "20px" }}
                            />
                          </Box>
                          <Typography
                            fontSize={12}
                            fontWeight={400}
                            color="#95919D"
                          >
                            Shared To:
                          </Typography>
                          <Box display="flex" gap={1}>
                            {_.postShareData?.hasInstagram && (
                              <img
                                src={insta}
                                alt="Instagram"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasLinkedin && (
                              <img
                                src={linkedin}
                                alt="LinkedIn"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasFacebook && (
                              <img
                                src={fb}
                                alt="Facebook"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasX && (
                              <img src={twit} alt="X" width={22} height={22} />
                            )}
                          </Box>
                        </Box>
                      </Box>
                    ))
                  ) : (
                    <Box textAlign={"center"}>No data found</Box>
                  )}
                </>
              )}
              {activeTab === "Posts" && (
                <>
                  {data?.length > 0 ? (
                    data?.map((_, index) => (
                      <Box
                        key={index}
                        sx={{
                          display: "flex",
                          gap: 2,
                          bgcolor: "#fff",
                          p: 2,
                          mb: 2,
                          borderRadius: "12px",
                          boxShadow: "0 1px 6px rgba(0,0,0,0.05)",
                          alignItems: "flex-start",
                          justifyContent: "space-between",
                          cursor: "pointer",
                        }}
                        onClick={(e) => handleDetails(_?.id, e)}
                      >
                        {/* Left: Image */}
                        {_?.media?.length > 0 ? (
                          <MediaCarousel media={_?.media || []} />
                        ) : (
                          <Box
                            sx={{
                              minWidth: "80px",
                              maxWidth: "80px",
                              height: "80px",
                              borderRadius: "8px",
                              overflow: "hidden",
                            }}
                          >
                            <img
                              src={noimg}
                              alt="post"
                              style={{
                                width: "100%",
                                height: "100%",
                                objectFit: "cover",
                              }}
                            />
                          </Box>
                        )}

                        {/* Middle: Texts */}
                        <Box sx={{ flex: 1 }}>
                          <Typography
                            variant="subtitle1"
                            sx={{
                              fontWeight: 400,
                              color: "#3B3B3B",
                              mb: 0.5,
                              fontSize: "16px",
                            }}
                          >
                            {_?.content || ""}
                          </Typography>

                          <Box
                            display="flex"
                            gap={1}
                            alignItems="center"
                            mt={1}
                          >
                            <img src={star} width={19} height={19} />
                            <Typography
                              fontSize={16}
                              fontWeight={400}
                              color="#3B3B3B"
                            >
                              {_?.earnedLoyaltyPoint || 0}
                            </Typography>
                          </Box>
                          <Typography fontSize={12} color="#2D76DC">
                            {_?.createdBy?.name || ""}
                          </Typography>
                        </Box>

                        {/* Right: Platforms & Arrow */}
                        <Box
                          sx={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 1,
                          }}
                        >
                          <Box justifyContent={"end"} display={"flex"}>
                            <NavigateNextIcon
                              sx={{ color: "#2D76DC", fontSize: "20px" }}
                            />
                          </Box>
                          <Typography
                            fontSize={12}
                            fontWeight={400}
                            color="#95919D"
                          >
                            Shared To:
                          </Typography>
                          <Box display="flex" gap={1}>
                            {_.postShareData?.hasInstagram && (
                              <img
                                src={insta}
                                alt="Instagram"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasLinkedin && (
                              <img
                                src={linkedin}
                                alt="LinkedIn"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasFacebook && (
                              <img
                                src={fb}
                                alt="Facebook"
                                width={22}
                                height={22}
                              />
                            )}
                            {_.postShareData?.hasX && (
                              <img src={twit} alt="X" width={22} height={22} />
                            )}
                          </Box>
                        </Box>
                      </Box>
                    ))
                  ) : (
                    <Box textAlign={"center"}>No data found</Box>
                  )}
                </>
              )}
              {activeTab === "Likes & Comments" && (
                <>
                  {[...Array(4)].map((_, index) => (
                    <Box
                      key={index}
                      sx={{
                        display: "flex",
                        gap: 2,
                        bgcolor: "#fff",
                        p: 2,
                        mb: 2,
                        borderRadius: "12px",
                        boxShadow: "0 1px 6px rgba(0,0,0,0.05)",
                        alignItems: "flex-start",
                        justifyContent: "space-between",
                      }}
                    >
                      {/* Left: Image */}
                      <Box
                        sx={{
                          minWidth: "80px",
                          maxWidth: "80px",
                          height: "80px",
                          borderRadius: "8px",
                          overflow: "hidden",
                        }}
                      >
                        <img
                          src={img1}
                          alt="post"
                          style={{
                            width: "100%",
                            height: "100%",
                            objectFit: "cover",
                          }}
                        />
                      </Box>

                      {/* Middle: Texts */}
                      <Box sx={{ flex: 1 }}>
                        <Typography
                          variant="subtitle1"
                          sx={{
                            fontWeight: 400,
                            color: "#3B3B3B",
                            mb: 0.5,
                            fontSize: "16px",
                          }}
                        >
                          Stay Ahead In The Digital Race!
                        </Typography>

                        <Box display="flex" gap={1} alignItems="center" mt={1}>
                          <img src={star} width={19} height={19} />
                          <Typography
                            fontSize={16}
                            fontWeight={400}
                            color="#3B3B3B"
                          >
                            150
                          </Typography>
                        </Box>
                        <Typography fontSize={12} color="#2D76DC">
                          Kiran Kumar
                        </Typography>
                      </Box>

                      {/* Right: Platforms & Arrow */}
                      <Box
                        sx={{
                          display: "flex",
                          flexDirection: "column",
                          gap: 1,
                        }}
                      >
                        <Box justifyContent={"end"} display={"flex"}>
                          <NavigateNextIcon
                            sx={{ color: "#2D76DC", fontSize: "20px" }}
                          />
                        </Box>
                        <Typography
                          fontSize={12}
                          fontWeight={400}
                          color="#95919D"
                        >
                          Shared To:
                        </Typography>
                        <Box display="flex" gap={1}>
                          <img src={insta} alt="insta" width={22} height={22} />
                          <img
                            src={linkedin}
                            alt="linkedin"
                            width={22}
                            height={22}
                          />
                          <img src={fb} alt="facebook" width={22} height={22} />
                          <img src={twit} alt="x" width={22} height={22} />
                        </Box>
                      </Box>
                    </Box>
                  ))}
                </>
              )}
            </Box>
          </DialogContent>
        </Box>
      </Dialog>
    </>
  );
};

export default ActivitiesPopup;
