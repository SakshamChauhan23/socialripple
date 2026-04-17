import React, { useState } from "react";
import { Box, Typography } from "@mui/material";
import TextareaAutosize from "@mui/material/TextareaAutosize";

const HashtagInput = ({value, onChange}) => {

  return (
    <Box mt={0}>
      <TextareaAutosize
        minRows={1}
        placeholder="#hashtags"
        // value={hashtags}
        value={value}
        // onChange={(e) => setHashtags(e.target.value)}
        onChange={(e) => onChange(e.target.value)}
        style={{
          width: "100%",
          border: "none",
          outline: "none",
          fontSize: "12px",
          fontWeight: 400,
          fontFamily: "inherit",
          resize: "none",
          color: "#333",
        }}
      />

      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ borderBottom: "1px solid #ccc", pb: 1 }}
      >
      </Typography>
    </Box>
  );
};

export default HashtagInput;
