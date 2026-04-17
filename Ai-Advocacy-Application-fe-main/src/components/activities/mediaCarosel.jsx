import React, { useState } from "react";
import { Box, IconButton } from "@mui/material";
import { ChevronLeft, ChevronRight, PlayArrow } from "@mui/icons-material";
import noImage from "../../assets/noimage.jpg";
import {
  getBunnyEmbedUrl,
  getResolvedImageUrl,
  getResolvedPosterUrl,
  getResolvedVideoUrl,
  isBunnyStreamMedia,
} from "../../shared/mediaResolver";

const MediaCarousel = ({ media }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isBunnyPlayerActive, setIsBunnyPlayerActive] = useState(false);

  const handlePrev = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === 0 ? media.length - 1 : prev - 1));
    setIsBunnyPlayerActive(false);
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === media.length - 1 ? 0 : prev + 1));
    setIsBunnyPlayerActive(false);
  };

  const currentMedia = media[currentIndex];

  return (
    <Box position="relative" display="flex" alignItems="center">
      {/* Left Button */}
      {media.length > 1 && (
        <IconButton
          onClick={(e) => handlePrev(e)}
          sx={{
            padding: "2px",
            position: "absolute",
            left: 0,
            zIndex: 1,
            background: "rgba(0,0,0,0.4)",
            color: "white",
            "&:hover": { background: "rgba(0,0,0,0.6)" },
          }}
        >
          <ChevronLeft sx={{ width: "10px" }} />
        </IconButton>
      )}

      {/* Media Box */}
      <Box
        sx={{
          minWidth: "80px",
          maxWidth: "80px",
          height: "80px",
          borderRadius: "8px",
          overflow: "hidden",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: "#000",
        }}
      >
        {currentMedia.mediaType === "IMAGE" ? (
          <img
            src={getResolvedImageUrl(currentMedia, noImage)}
            alt="post"
            style={{
              width: "100%",
              height: "100%",
              objectFit: "cover",
            }}
          />
        ) : isBunnyStreamMedia(currentMedia) ? (
          isBunnyPlayerActive ? (
            <iframe
              src={getBunnyEmbedUrl(getResolvedVideoUrl(currentMedia))}
              title="post video"
              style={{
                width: "100%",
                height: "100%",
                border: 0,
                display: "block",
              }}
              allow="accelerometer; gyroscope; encrypted-media; picture-in-picture;"
              allowFullScreen
            />
          ) : (
            <Box sx={{ position: "relative", width: "100%", height: "100%" }}>
              <img
                src={getResolvedPosterUrl(currentMedia, noImage)}
                alt="video poster"
                style={{
                  width: "100%",
                  height: "100%",
                  objectFit: "cover",
                }}
              />
              <IconButton
                onClick={(e) => {
                  e.stopPropagation();
                  setIsBunnyPlayerActive(true);
                }}
                sx={{
                  position: "absolute",
                  top: "50%",
                  left: "50%",
                  transform: "translate(-50%, -50%)",
                  background: "rgba(0,0,0,0.5)",
                  color: "#fff",
                  "&:hover": { background: "rgba(0,0,0,0.7)" },
                }}
              >
                <PlayArrow sx={{ width: "14px" }} />
              </IconButton>
            </Box>
          )
        ) : (
          <video
            src={getResolvedVideoUrl(currentMedia)}
            poster={getResolvedPosterUrl(currentMedia)}
            controls
            style={{
              width: "100%",
              height: "100%",
              objectFit: "cover",
            }}
          />
        )}
      </Box>

      {/* Right Button */}
      {media.length > 1 && (
        <IconButton
          onClick={(e) => handleNext(e)}
          sx={{
            padding: "2px",
            position: "absolute",
            right: 0,
            zIndex: 1,
            background: "rgba(0,0,0,0.4)",
            color: "white",
            "&:hover": { background: "rgba(0,0,0,0.6)" },
          }}
        >
          <ChevronRight sx={{ width: "10px" }} />
        </IconButton>
      )}
    </Box>
  );
};

export default MediaCarousel;
