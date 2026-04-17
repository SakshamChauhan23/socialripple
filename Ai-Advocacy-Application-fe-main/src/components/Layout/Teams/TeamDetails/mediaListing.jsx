
import React, { useRef, useState } from "react";
import { CardMedia, Box, IconButton } from "@mui/material";
import { ChevronLeft, ChevronRight } from "@mui/icons-material";
import noImage from "../../../../assets/noimage.jpg";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import PauseIcon from "@mui/icons-material/Pause";

const PostMediaViewer = ({ media = [] }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const validMedia = media?.filter((item) => item?.fileUrl);
  const currentMedia = validMedia[currentIndex] || {};

  const videoRef = useRef(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const handlePrev = (e) => {
    e.stopPropagation(); // prevent triggering handleDetails
    setCurrentIndex((prev) => (prev === 0 ? validMedia.length - 1 : prev - 1));
  };

  const handleNext = (e) => {
    e.stopPropagation(); // prevent triggering handleDetails
    setCurrentIndex((prev) => (prev === validMedia.length - 1 ? 0 : prev + 1));
  };

  const handlePlay = (e) => {
    e.stopPropagation(); // prevent triggering handleDetails
    const video = videoRef.current;
    if (!video) return;

    if (isPlaying) {
      video.pause();
      setIsPlaying(false);
    } else {
      video.play();
      setIsPlaying(true);
    }
  };

  return (
    <Box position="relative">
      {currentMedia?.mediaType === "VIDEO" ? (
        <Box position="relative">
          <CardMedia
            component="video"
            ref={videoRef}
            height="250"
            src={currentMedia?.fileUrl || noImage}
            sx={{
              objectFit: "cover",
              width: "100%",
              borderRadius: "8px",
              cursor: "pointer",
            }}
            onEnded={() => setIsPlaying(false)}
          />

          {/* Play/Pause Button */}
          <IconButton
            onClick={handlePlay}
            sx={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              backgroundColor: "rgba(0,0,0,0.6)",
              color: "white",
              "&:hover": { backgroundColor: "rgba(0,0,0,0.8)" },
              width: 60,
              height: 60,
            }}
          >
            {isPlaying ? (
              <PauseIcon fontSize="large" />
            ) : (
              <PlayArrowIcon fontSize="large" />
            )}
          </IconButton>
        </Box>
      ) : (
        <CardMedia
          component="img"
          height="250"
          image={currentMedia?.fileUrl || noImage}
          alt="Post Media"
          sx={{ objectFit: "cover", cursor: "pointer" }}
        />
      )}

      {/* Navigation Buttons */}
      {validMedia?.length > 1 && (
        <>
          <IconButton
            onClick={handlePrev}
            sx={{
              position: "absolute",
              left: 8,
              top: "50%",
              transform: "translateY(-50%)",
              backgroundColor: "rgba(0,0,0,0.4)",
              color: "#fff",
              "&:hover": { backgroundColor: "rgba(0,0,0,0.6)" },
            }}
          >
            <ChevronLeft />
          </IconButton>

          <IconButton
            onClick={handleNext}
            sx={{
              position: "absolute",
              right: 8,
              top: "50%",
              transform: "translateY(-50%)",
              backgroundColor: "rgba(0,0,0,0.4)",
              color: "#fff",
              "&:hover": { backgroundColor: "rgba(0,0,0,0.6)" },
            }}
          >
            <ChevronRight />
          </IconButton>
        </>
      )}
    </Box>
  );
};

export default PostMediaViewer;
