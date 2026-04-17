import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  Typography,
  IconButton,
  Avatar,
  Box,
  ToggleButtonGroup,
  ToggleButton,
  TextField,
  Button,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { styled } from "@mui/system";
import Carousel from "react-material-ui-carousel";

const StyledToggleButtonGroup = styled(ToggleButtonGroup)({
  display: "flex",
  justifyContent: "flex-end",
  background: "#F5F5F9",
  borderRadius: "20px",
  padding: "5px",
  width: "fit-content",
  margin: "0 10px",
  position: "absolute",
  top: "10px",
  right: "10px",
});

const StyledToggleButton = styled(ToggleButton)({
  border: "none",
  borderRadius: "20px",
  padding: "8px 20px",
  textTransform: "none",
  fontWeight: "bold",
  transition: "background 0.3s",
  "&.Mui-selected": {
    background: "#fff",
    boxShadow: "0px 4px 6px rgba(0, 0, 0, 0.1)",
  },
  "&:not(:last-child)": {
    marginRight: "8px",
  },
});

const PostDialog = ({ isOpen, onClose, post }) => {
  const [tabIndex, setTabIndex] = useState("likes");
  const [comment, setComment] = useState("");
  const [LoggedInUser, setLoggedInUser] = useState(null);
  const [comments, setComments] = useState([]);

  useEffect(() => {
    setComments(post?.comments || []);
  }, [post]);

  useEffect(() => {
    const storedUser = localStorage.getItem("userProfile");
    if (storedUser) {
      setLoggedInUser(JSON.parse(storedUser));
    }
  }, []);

  const handleAddComment = () => {
    if (comment.trim()) {
      const newComment = {
        user: {
          avatar: LoggedInUser?.profilePhotoUrl,
          name: LoggedInUser?.firstName,
        },
        text: comment,
      };

      const updatedComments = [...comments, newComment];
      setComments(updatedComments);
      post.comments = updatedComments;
      setComment("");
    }
  };

  return (
    <Dialog open={isOpen} onClose={onClose} fullWidth maxWidth="xlg">
      <Box position="relative" p={2}>
        {/* Header Section */}
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Box display="flex" alignItems="center">
            <Avatar src={post?.user?.avatar} alt={post?.user?.name} />
            <Box ml={2}>
              <Typography fontWeight="bold">{post?.user?.name}</Typography>
              <Typography variant="body2" color="textSecondary">
                {post?.user?.role}
              </Typography>
            </Box>
          </Box>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>

        {/* Tabs Positioned at the Top Right */}
        <StyledToggleButtonGroup
          value={tabIndex}
          exclusive
          onChange={(e, newValue) => setTabIndex(newValue)}
          style={{
            marginTop: "4rem",
            width: "48%",
            display: "flex",
            justifyContent: "space-evenly",
          }}
        >
          <StyledToggleButton value="likes">
            Likes ({post?.likes?.length})
          </StyledToggleButton>
          <StyledToggleButton value="comments">
            Comments ({post?.comments?.length})
          </StyledToggleButton>
          <StyledToggleButton value="shares">
            Shares ({post?.shares})
          </StyledToggleButton>
          <StyledToggleButton value="insights">Insights</StyledToggleButton>
        </StyledToggleButtonGroup>
      </Box>

      {/* Main Content - 50/50 Split */}
      <DialogContent>
        <Box display="flex" width="100%">
          {/* Post Image/Carousel - 50% */}
          <Box flex={1} position="relative" pr={2}>
            <Typography variant="h6">{post.title}</Typography>
            <Typography variant="body2">{post.description}</Typography>
            {post?.images?.length > 1 ? (
              <Carousel>
                {post?.images?.map((img, index) => (
                  <img
                    key={index}
                    src={img}
                    alt={`Slide ${index}`}
                    style={{
                      width: "100%",
                      height: "250px",
                      borderRadius: "8px",
                    }}
                  />
                ))}
              </Carousel>
            ) : (
              <img
                src={post?.images?.[0]}
                alt="Post"
                style={{ width: "100%", borderRadius: "8px" }}
              />
            )}
          </Box>

          {/* Engagement Content - 50% */}
          <Box
            flex={1}
            display="flex"
            flexDirection="column"
            justifyContent="center"
          >
            <Box mt={-14}>
              {/* Likes Tab */}
              {tabIndex === "likes" && (
                <Box>
                  {post?.likes?.map((user, index) => (
                    <Box
                      key={index}
                      display="flex"
                      alignItems="center"
                      mb={2}
                      style={{
                        backgroundColor: "#F5F5F9",
                        borderRadius: "8px",
                        padding: "10px",
                      }}
                    >
                      <Avatar src={user.avatar} alt={user.name} />
                      <Box ml={1}>
                        <Typography
                          style={{
                            color: "#3B3B3B",
                            fontSize: "16px",
                            fontWeight: 500,
                          }}
                        >
                          {user.name}
                        </Typography>
                        <Typography
                          variant="body2"
                          color="textSecondary"
                          style={{
                            fontSize: "12px",
                            color: "#95919D",
                            marginLeft: "5px",
                          }}
                        >
                          {user.role}
                        </Typography>
                      </Box>
                    </Box>
                  ))}
                </Box>
              )}

              {/* Comments Tab */}
              {tabIndex === "comments" && (
                <Box>
                  {/* Existing Comments */}
                  {post?.comments?.map((comment, index) => (
                    <Box key={index} display="flex" alignItems="center" mb={2}>
                      {comment.profilePhotoUrl ? (
                        <Avatar
                          src={comment.user.profilePhotoUrl}
                          alt={comment.user.name}
                        />
                      ) : (
                        <Avatar>{comment?.user?.name?.[0]}</Avatar>
                      )}
                      <Box ml={2}>
                        <Typography fontWeight="bold">
                          {comment.user.name}
                        </Typography>
                        <Typography variant="body2" color="textSecondary">
                          {comment.text}
                        </Typography>
                      </Box>
                    </Box>
                  ))}

                  {/* Add Comment Input */}
                  <Box display="flex" alignItems="center" mt={2}>
                    {LoggedInUser?.profilePhotoUrl ? (
                      <Avatar
                        src={LoggedInUser.profilePhotoUrl}
                        alt={LoggedInUser.firstName}
                      />
                    ) : (
                      <Avatar>{LoggedInUser?.firstName?.[0]}</Avatar>
                    )}
                    <TextField
                      fullWidth
                      variant="outlined"
                      placeholder="Add a comment..."
                      size="small"
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                      style={{ marginLeft: "10px" }}
                    />
                    <Button
                      variant="contained"
                      color="primary"
                      style={{ marginLeft: "10px" }}
                      onClick={handleAddComment}
                    >
                      Post
                    </Button>
                  </Box>
                </Box>
              )}

              {/* Shares Tab */}
              {tabIndex === "shares" && <Typography>No shares yet.</Typography>}

              {/* Insights Tab */}
              {tabIndex === "insights" && (
                <Typography>Insights content goes here...</Typography>
              )}
            </Box>
          </Box>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default PostDialog;
