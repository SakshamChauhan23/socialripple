import React from "react";
import {
  Dialog,
  DialogContent,
  IconButton,
  Typography,
  Box,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import "./StatusPopup.css";

const StatusPopup = ({ open, onClose }) => {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xs"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: "20px",
          padding: "24px",
          position: "relative",
          textAlign: "center",
        },
      }}
    >
      <IconButton
        onClick={onClose}
        sx={{
          position: "absolute",
          top: 12,
          right: 12,
          zIndex: 1,
        }}
      >
        <CloseIcon sx={{ color: "#000" }} />
      </IconButton>

      <DialogContent
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          padding: "32px 24px",
        }}
      >
        <Box className="tea-cup-container">
          <Box className="tea-cup">
            <span role="img" aria-label="tea cup" className="tea-emoji">
              ☕
            </span>
          </Box>
        </Box>

        <Typography
          sx={{
            color: "#3B3B3B",
            fontSize: "18px",
            fontWeight: 500,
            marginTop: "24px",
            lineHeight: 1.5,
          }}
        >
          We're working on your post — till then, have a cup of tea.
        </Typography>
      </DialogContent>
    </Dialog>
  );
};

export default StatusPopup;
