import React from "react";
import { Box, Avatar, InputBase, Button } from "@mui/material";
import avt from "../../../assets/avt.png";
const CommentBox = ({value, onChange}) => {
  return (
    <Box display="flex" alignItems="center" gap={2}>
      {/* Avatar */}
      <Avatar
        alt="User"
        src={avt} 
        sx={{ width: 50, height: 50, border: "1px solid #2D76DC" }}
      />

      {/* Comment Input + Button */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          flex: 1,
          border: "0.4px solid #8C8C8C",
          borderRadius: "16px",
          pl: 2,
          pr: 1,
          py: 0.8,
        }}
      >
        <InputBase
          placeholder="What Is Your Thought?"
          fullWidth
          value={value}
          onChange={onChange}
          sx={{
            fontSize: 14,
            color: "#555",
          }}
        />
        <Button
          variant="contained"
          sx={{
            borderRadius: "8px",
            textTransform: "none",
            ml: 1,
            px: 3,
            py: 1,
            backgroundColor: "#0047AB",
            "&:hover": {
              backgroundColor: "#084dbf",
            },
          }}
        >
          Comment
        </Button>
      </Box>
    </Box>
  );
};

export default CommentBox;
