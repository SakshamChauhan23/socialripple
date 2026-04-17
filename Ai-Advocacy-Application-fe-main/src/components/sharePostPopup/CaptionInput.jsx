import React, { useState } from "react";
import { Box, Typography } from "@mui/material";
import TextareaAutosize from '@mui/material/TextareaAutosize'; // ✅ correct import

const CleanCaptionInput = ({value, onChange}) => {

  return (
    <Box>
      <TextareaAutosize
        // value={caption}
        value={value}
        // onChange={(e) => setCaption(e.target.value)}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Write your caption..."
        minRows={2}
        style={{
          width: "100%",
          border: "none",
          outline: "none",
          fontSize: "12px",
          fontWeight: 400,
          fontFamily: "'Inter', sans-serif",
          resize: "none",
          color: "#3B3B3B",
          lineHeight: "1.6",
        }}
      />

      <Typography
        variant="body2"
        color="#999"
        sx={{ borderBottom: "1px solid #ccc", pb: 1 }}
      >
      </Typography>
    </Box>
  );
};

export default CleanCaptionInput;
