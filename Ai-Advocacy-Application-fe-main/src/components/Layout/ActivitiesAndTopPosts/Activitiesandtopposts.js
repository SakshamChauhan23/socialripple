import React, { useState } from "react";
import {
    Card,
    CardContent,
    Typography,
    Tabs,
    Tab,
    Button,
    Box,
} from "@mui/material";
import { Instagram, LinkedIn, Facebook, ThumbUp, Comment } from "@mui/icons-material";
import ArrowForwardIosIcon from "@mui/icons-material/ArrowForwardIos";
import "./Activitiesandtopposts.css";

const ActivitiesAndTopPosts = () => {
    const sharesData = [
        { time: "2 min ago", content: "Stay Ahead in The Digital Race!", user: "John Doe", platform: [<Instagram />, <LinkedIn />, <Facebook />] },
        { time: "3 hrs ago", content: "Understanding Material-UI!", user: "Jane Smith", platform: [<Facebook />] },
    ];

    const postsData = [
        { time: "1 day ago", content: "Exploring React Best Practices!", user: "Emily Clark" },
        { time: "Last Week", content: "Understanding Material-UI!", user: "Michael Brown" },
    ];

    const likesCommentsData = [
        { time: "10 min ago", content: "Liked your post: Mastering Frontend!", user: "Sarah Wilson", liked: true, commented: false },
        { time: "Yesterday", content: "Commented on your post: Best UX Practices!", user: "David Lee", liked: false, commented: true },
    ];

    // const [mainTab, setMainTab] = useState("activities");
    const [activitiesTab, setActivitiesTab] = useState(0);
    const [loadedShares, setLoadedShares] = useState(sharesData);
    const [loadedPosts, setLoadedPosts] = useState(postsData);
    const [loadedLikesComments, setLoadedLikesComments] = useState(likesCommentsData);

    // const handleMainTabToggle = (newTab) => setMainTab(newTab);
    const handleActivitiesTabChange = (event, newValue) => setActivitiesTab(newValue);

    const loadMoreItems = (type) => {
        if (type === "shares") {
            setLoadedShares([...loadedShares, ...sharesData]);
        } else if (type === "posts") {
            setLoadedPosts([...loadedPosts, ...postsData]);
        } else if (type === "likesComments") {
            setLoadedLikesComments([...loadedLikesComments, ...likesCommentsData]);
        }
    };

    const renderCard = (item, type) => (
        <Box key={item.content} sx={{ marginBottom: 2 }}>
            {/* Time Display Above Card */}
            <Typography variant="caption" color="text.secondary" sx={{ marginBottom: 0.5 }}>
                {item.time}
            </Typography>

            <Card sx={{ display: "flex", alignItems: "center", padding: 2, borderRadius: 4 }}>
                {/* Left Side - Post Image */}
                <Box sx={{ position: "relative", width: 100, height: 100, flexShrink: 0 }}>
                    <img
                        src={item.image}
                        alt="postImage"
                        sx={{ width: "100%", height: "100%", borderRadius: 2 }}
                    />
                </Box>

                {/* Right Side - Post Content */}
                <Box sx={{ flexGrow: 1, paddingLeft: 2 , position:"relative"}}>
                    {/* Post Title */}
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                        {item.content}
                    </Typography>

                    {/* Small Arrow in Right Top Corner */}
                    <Box sx={{ position: "absolute", top: 8, right: 8 }}>
                        <ArrowForwardIosIcon  fontSize="small" sx={{ fontSize: 14, color: "gray" }} />
                    </Box>

                    {/* User Name */}
                    <Typography variant="body2" color="primary" sx={{ marginTop: 0.5 }}>
                        {item.user}
                    </Typography>

                    {/* Bottom Right Section: Social Media Icons or Like/Comment */}
                    <Box sx={{ display: "flex", justifyContent: "flex-end", marginTop: 1 }}>
                        {type === "shares" && (
                            <Box sx={{ display: "flex", gap: 1 }}>
                                {item.platform.map((icon, index) => (
                                    <Button key={index} size="small" sx={{ minWidth: "unset" }}>
                                        {icon}
                                    </Button>
                                ))}
                            </Box>
                        )}

                        {type === "likesComments" && (
                            <Box sx={{ display: "flex", gap: 2 }}>
                                <ThumbUp fontSize="small" />
                                <Comment fontSize="small" />
                            </Box>
                        )}
                    </Box>
                </Box>
            </Card>
        </Box>
    );


    return (
        <Card className="activities-container" >
            <CardContent>
                {/* Toggle for Activities and Top Posts */}
                {/* <Box
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        position: "relative",
                        width: "100%",
                        border: "1px solid #ccc",
                        borderRadius: 20,
                        padding: "4px",
                        backgroundColor: "#F5F5F9",
                    }}
                >
                    <Box
                        sx={{
                            position: "absolute",
                            top: 4,
                            left: mainTab === "activities" ? 4 : "50%",
                            width: "45%",
                            height: "calc(100% - 8px)",
                            backgroundColor: "#FFFFFF",
                            borderRadius: 20,
                            transition: "left 0.3s ease",
                        }}
                    ></Box>
                    <Button
                        onClick={() => handleMainTabToggle("activities")}
                        className="mainTab"
                        sx={{
                            fontWeight: mainTab === "activities" ? 600 : 400,
                        }}
                    >
                        Activities
                    </Button>
                    <Button
                        onClick={() => handleMainTabToggle("topPosts")}
                        className="mainTab"
                        sx={{
                            fontWeight: mainTab === "topPosts" ? 600 : 400,
                        }}
                    >
                        Top Posts
                    </Button>
                </Box> */}
                <Box className="activities-title">Activities</Box>
                <Box mt={2}>
                    <Tabs
                        value={activitiesTab}
                        onChange={handleActivitiesTabChange}
                        variant="fullWidth"
                        className="activityTabs"
                    >
                        <Tab label="Shares" sx={{ minWidth: 0, width: '25%' }} />
                        <Tab label="Posts" sx={{ minWidth: 0, width: '25%' }} />
                        <Tab label="Likes & Comments" sx={{ minWidth: 0, width: '25%' }} />
                    </Tabs>

                    {activitiesTab === 0 &&
                        loadedShares.map((item) => renderCard(item, "shares"))}

                    {activitiesTab === 1 &&
                        loadedPosts.map((item) => renderCard(item, "posts"))}

                    {activitiesTab === 2 &&
                        loadedLikesComments.map((item) => renderCard(item, "likesComments"))}

                    <Button
                        variant="outlined"
                        onClick={() =>
                            loadMoreItems(
                                activitiesTab === 0
                                    ? "shares"
                                    : activitiesTab === 1
                                        ? "posts"
                                        : "likesComments"
                            )
                        }
                        sx={{ width: "100%", marginTop: 2 }}
                    >
                        Show More
                    </Button>
                </Box>
            </CardContent>
        </Card>
    );
};

export default ActivitiesAndTopPosts;
