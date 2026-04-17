import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  TextField,
  MenuItem,
  Grid,
  Box,
  Typography,
  Button,
  Checkbox,
  InputBase,
  useTheme,
  useMediaQuery,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import SearchIcon from "@mui/icons-material/Search";
import { useEffect, useState } from "react";
import "./Postcreation.css";
import { Auth } from "../../../contexts/AuthContext";
import { getAllMediaByType } from "../../../services/postService";

const VideoLibraryModal = ({ open, onClose, onConfirm }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const [searchTerm, setSearchTerm] = useState("");
  const [category, setCategory] = useState("");
  const [selected, setSelected] = useState([]);
  const categoriesList = Auth();
  const [playingIndex, setPlayingIndex] = useState(null);
  const [uploadedVideos, setUploadedVideos] = useState([]);
  const [apiVideos, setApiVideos] = useState([]);
  const toggleSelect = (video) => {
    setSelected((prev) =>
      prev.find((v) => v.preview === video.preview)
        ? prev.filter((v) => v.preview !== video.preview)
        : prev.length < 6
        ? [...prev, video]
        : prev
    );
  };

  const handleDeviceUpload = (e) => {
    const files = Array.from(e.target.files);
    const newVideoObjects = files.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
      isNew: true, // mark as new (needs upload)
    }));
    setUploadedVideos((prev) => [...prev, ...newVideoObjects]);
    setSelected((prev) => [...prev, ...newVideoObjects]);
  };

  // const allVideos = [...sampleVideos, ...uploadedVideos.map((v) => v.preview)];
  const allVideos = [...apiVideos, ...uploadedVideos];
  const handlePlayVideo = (index) => {
    const allVideoEls = document.querySelectorAll(".library-video");

    allVideoEls.forEach((vid, i) => {
      if (i === index) {
        vid.play();
        setPlayingIndex(index);
      } else {
        vid.pause();
      }
    });
  };

  const handlePauseVideo = (index) => {
    const videoEl = document.querySelectorAll(".library-video")[index];
    if (videoEl) {
      videoEl.pause();
      setPlayingIndex(null);
    }
  };

  const fetchAllMedia = async () => {
    try {
      const payload = {
        type: "video",
        categoryId: category,
        search: searchTerm,
      };
      const response = await getAllMediaByType(payload);
      if (response?.status === true) {
        const mapped = response?.mediaLibrary?.map((item) => ({
          id: item.id,
          url: item.url,
          preview: item.url, // preview ke liye url hi use hoga
          isNew: false,
        }));
        setApiVideos(mapped);
      }
    } catch (error) {
      console.log(error);
    }
  };

  useEffect(() => {
    fetchAllMedia();
  }, [category]);
  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="md"
      fullScreen={isMobile}
    >
      <DialogTitle sx={{ fontWeight: 500, fontSize: 18 }}>
        Select A Video From Library
        <IconButton
          onClick={onClose}
          sx={{ position: "absolute", right: 16, top: 16 }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent>
        <Box
          display="flex"
          gap={2}
          width={"100%"}
          alignItems="flex-start"
          mb={3}
        >
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              backgroundColor: "#f7f8fa",
              border: "1px solid #e0e0e0",
              borderRadius: "6px",
              px: 2,
              height: 40,
              width: "50%",
              mt: 3.3,
            }}
          >
            <SearchIcon sx={{ color: "#1976d2", mr: 1 }} />
            <InputBase
              placeholder="Search"
              fullWidth
              onChange={(e) => setSearchTerm(e.target.value)}
              sx={{ color: "#666", fontSize: "16px" }}
            />
          </Box>

          {/* Category Dropdown */}
          <Box width={"50%"}>
            <Typography
              variant="body2"
              sx={{ color: "#9e9e9e", mb: "6px", ml: "2px" }}
            >
              Category
            </Typography>
            <TextField
              select
              size="small"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              sx={{
                width: "100%",
                height: "60px",
                backgroundColor: "#",
                "& .MuiOutlinedInput-root": {
                  borderRadius: "8px",
                  border: "1px solid #DAE1EB",
                },
                "& .MuiInputBase-input": {
                  color: "#000000",
                  background: "#FCFCFC",
                  padding: "8.8px 14px",
                },
                "& .MuiSvgIcon-root": {
                  color: "#2D76DC",
                },
              }}
              MenuProps={{
                PaperProps: {
                  style: {
                    maxHeight: 88 * 4.5 + 8,
                    width: '100%',
                  },
                },
                anchorOrigin: {
                  vertical: "bottom",
                  horizontal: "left",
                },
                transformOrigin: {
                  vertical: "top",
                  horizontal: "left",
                },
                getContentAnchorEl: null, // Ensures the menu anchors below the select
              }}
            >
              <MenuItem value="All Category">All Category</MenuItem>
              {categoriesList?.categoriesData?.map((cat) => (
                <MenuItem key={cat?.id} value={cat?.id}>
                  {cat?.name}
                </MenuItem>
              ))}
            </TextField>
          </Box>
        </Box>
        <Box display="flex" justifyContent="flex-end" mb={1}>
          <Typography>{`Selected: ${selected.length}/6`}</Typography>
        </Box>

        <Box sx={{ maxHeight: 400, overflowY: "auto" }}>
          <Grid container spacing={2}>
            {allVideos.map((video, index) => (
              <Grid item xs={4} key={index}>
                <Box
                  onClick={() => toggleSelect(video)}
                  sx={{
                    position: "relative",
                    borderRadius: 2,
                    overflow: "hidden",
                    border: selected.includes(video)
                      ? "2px solid #0047AB"
                      : "2px solid transparent",
                    cursor: "pointer",
                  }}
                >
                  <video
                    src={video.preview}
                    width="100%"
                    height="150px"
                    style={{ objectFit: "cover" }}
                    muted
                    className="library-video"
                    preload="metadata"
                    onMouseOver={(e) => e.target.play()}
                    onMouseOut={(e) => {
                      e.target.pause();
                      e.target.currentTime = 0;
                    }}
                    onClick={() =>
                      playingIndex === index
                        ? handlePauseVideo(index)
                        : handlePlayVideo(index)
                    }
                  />
                  {!video.playing && (
                    <Box
                      className="playButton"
                      onClick={(e) => {
                        e.stopPropagation();
                        handlePlayVideo(index);
                      }}
                    >
                      <Box className="playIcon">
                        <PlayArrowIcon
                          style={{ fontSize: 35, color: "#0047AB" }}
                        />
                      </Box>
                    </Box>
                  )}
                  <Checkbox
                    checked={selected.includes(video)}
                    onClick={(e) => {
                      e.stopPropagation(); // Don’t bubble to video play
                      toggleSelect(video);
                    }}
                    sx={{
                      position: "absolute",
                      top: 8,
                      left: 8,
                      color: "#fff",
                    }}
                  />
                  {/* {selected.includes(video) && (
                    <Checkbox
                      checked
                      sx={{ position: "absolute", top: 8, left: 8 }}
                    />
                  )} */}
                </Box>
              </Grid>
            ))}
            {/* {allVideos.map((video, index) => (
              <Grid item xs={4} key={index}>
                <Box
                  onClick={() => toggleSelect(video)}
                  sx={{
                    border: selected.includes(video)
                      ? "2px solid #0047AB"
                      : "2px solid transparent",
                  }}
                >
                  <video
                    src={video.preview}
                    width="100%"
                    height="200px"
                    muted
                    className="library-video"
                  />
                  <Checkbox
                    checked={selected.includes(video)}
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleSelect(video);
                    }}
                    sx={{ position: "absolute", top: 8, left: 8 }}
                  />
                </Box>
              </Grid>
            ))} */}
          </Grid>
        </Box>

        <Box mt={3} display="flex" justifyContent="space-between">
          <Button
            variant="outlined"
            sx={{
              backgroundColor: "#F4F0FF",
              color: "#3B3B3B",
              textTransform: "none",
              borderRadius: 2,
              px: 3,
              fontWeight: 500,
              border: "none",
            }}
            onClick={() =>
              document.getElementById("video-library-upload").click()
            }
          >
            Upload from Device
          </Button>

          <Box display="flex" gap={2}>
            <Button
              onClick={onClose}
              sx={{
                backgroundColor: "#f6f1ff",
                color: "#0047AB",
                textTransform: "none",
                borderRadius: 2,
                px: 4,
                fontWeight: 500,
              }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={() => {
                onConfirm(selected);
                onClose();
              }}
              sx={{
                backgroundColor: "#0047AB",
                textTransform: "none",
                borderRadius: 2,
                px: 4,
              }}
            >
              Confirm
            </Button>
          </Box>
        </Box>

        <input
          type="file"
          id="video-library-upload"
          accept="video/*"
          hidden
          multiple
          onChange={handleDeviceUpload}
        />
      </DialogContent>
    </Dialog>
  );
};

export default VideoLibraryModal;
