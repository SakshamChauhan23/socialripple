import React, { useState, useEffect, useRef } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  Typography,
  Box,
  TextField,
  InputAdornment,
  Grid,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import insta from "../../../assets/socialmedia/instagram.svg";
import facebook from "../../../assets/socialmedia/faceBuk.svg";
import linkedin from "../../../assets/socialmedia/linkdn.svg";
import x from "../../../assets/socialmedia/x.svg";

const platformOptions = [
  { name: "Instagram", key: "instagram", icon: insta },
  { name: "Facebook", key: "facebook", icon: facebook },
  { name: "Twitter / X", key: "x", icon: x },
  { name: "LinkedIn", key: "linkedin", icon: linkedin },
];

const EditScheduleModal = ({ open, onClose, scheduleData, onSave }) => {
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const dateInputRef = useRef(null);
  const timeInputRef = useRef(null);
  const [selectedPlatforms, setSelectedPlatforms] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (scheduleData) {
      if (scheduleData.scheduledTimeUtc) {
        const utcDate = new Date(scheduleData.scheduledTimeUtc);
        const istDateStr = utcDate.toLocaleDateString("en-CA", {
          timeZone: "Asia/Kolkata",
        });
        const istTimeStr = utcDate.toLocaleTimeString("en-GB", {
          timeZone: "Asia/Kolkata",
          hour: "2-digit",
          minute: "2-digit",
          hour12: false,
        });
        setDate(istDateStr);
        setTime(istTimeStr);
      }

      // Initialize platforms from scheduleData
      if (scheduleData.platforms && Array.isArray(scheduleData.platforms)) {
        const normalizedPlatforms = scheduleData.platforms.map((p) => {
          const lower = p.toLowerCase();
          if (lower === "twitter" || lower === "twitter/x") {
            return "x";
          }
          return lower;
        });
        setSelectedPlatforms(normalizedPlatforms);
      } else {
        setSelectedPlatforms([]);
      }
    }
  }, [scheduleData]);

  const handlePlatformToggle = (platformKey) => {
    setSelectedPlatforms((prev) =>
      prev.includes(platformKey)
        ? prev.filter((p) => p !== platformKey)
        : [...prev, platformKey]
    );
  };

  const handleSave = async () => {
    if (!date || !time || selectedPlatforms.length === 0) {
      return;
    }

    // Combine date + time into ISO format
    const combinedDateTime = new Date(`${date}T${time}:00Z`);

    // Convert platform keys to uppercase for API
    const platformsForApi = selectedPlatforms.map((p) => p.toUpperCase());

    setLoading(true);
    try {
      await onSave({
        ...scheduleData,
        scheduledTimeUtc: combinedDateTime.toISOString().split(".")[0] + "Z",
        platforms: platformsForApi,
      });
    } finally {
      setLoading(false);
    }
  };

  const getMinDate = () => {
    const now = new Date();
    return now.toLocaleDateString("en-CA", {
      timeZone: "Asia/Kolkata",
    });
  };

  const isFormValid = date && time && selectedPlatforms.length > 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: "20px",
          padding: "24px",
          position: "relative",
        },
      }}
    >
      <IconButton
        onClick={onClose}
        sx={{ position: "absolute", top: 12, right: 12 }}
      >
        <CloseIcon sx={{ color: "#000" }} />
      </IconButton>

      <DialogTitle
        sx={{
          fontWeight: "500",
          color: "#3B3B3B",
          fontSize: "24px",
          p: 0,
          mb: 1,
        }}
      >
        Edit Scheduled Post
      </DialogTitle>

      <DialogContent sx={{ px: 0, pt: 2 }}>
        {/* Content - Read Only */}
        <Box sx={{ mb: 3 }}>
          <Typography
            sx={{ color: "#3B3B3B", fontSize: "14px", fontWeight: 500, mb: 1 }}
          >
            Content
          </Typography>
          <Box
            sx={{
              backgroundColor: "#f5f5f5",
              borderRadius: "8px",
              padding: "12px",
              minHeight: "80px",
              color: "#3B3B3B",
              fontSize: "14px",
            }}
          >
            {scheduleData?.content || "No content"}
          </Box>
        </Box>

        {/* Platforms Selection */}
        <Box sx={{ mb: 3 }}>
          <Typography
            sx={{ color: "#3B3B3B", fontSize: "14px", fontWeight: 500, mb: 1 }}
          >
            Platforms
          </Typography>
          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
            {platformOptions.map((platform) => (
              <Box
                key={platform.key}
                onClick={() => handlePlatformToggle(platform.key)}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  padding: "8px 12px",
                  borderRadius: "8px",
                  border: selectedPlatforms.includes(platform.key)
                    ? "2px solid #0047AB"
                    : "1px solid #e0e0e0",
                  backgroundColor: selectedPlatforms.includes(platform.key)
                    ? "#F4F0FF"
                    : "#f5f5f5",
                  cursor: "pointer",
                  transition: "all 0.2s",
                  "&:hover": {
                    backgroundColor: "#ece9fd",
                  },
                }}
              >
                <img
                  src={platform.icon}
                  alt={platform.name}
                  style={{ width: 20, height: 20 }}
                />
                <Typography sx={{ fontSize: "13px", color: "#3B3B3B" }}>
                  {platform.name}
                </Typography>
              </Box>
            ))}
          </Box>
        </Box>

        {/* Scheduled Time */}
        <Box sx={{ display: "flex", gap: 2 }}>
          <Grid item xs={6} sx={{ flex: 1, position: "relative" }}>
            <Typography
              sx={{
                color: "#3B3B3B",
                fontSize: "14px",
                fontWeight: 500,
                mb: 1,
              }}
            >
              Date
            </Typography>
            <TextField
              fullWidth
              value={date}
              variant="outlined"
              onClick={() => dateInputRef.current?.showPicker()}
              InputProps={{
                readOnly: true,
                startAdornment: (
                  <InputAdornment position="start">
                    <label htmlFor="edit-schedule-date-picker">
                      <CalendarMonthOutlinedIcon
                        sx={{ color: "#1976d2", cursor: "pointer" }}
                      />
                    </label>
                  </InputAdornment>
                ),
              }}
              sx={{
                "& .MuiOutlinedInput-root": {
                  borderRadius: "8px",
                  backgroundColor: "#f5f5f5",
                },
              }}
            />
            <input
              type="date"
              id="edit-schedule-date-picker"
              ref={dateInputRef}
              value={date}
              min={getMinDate()}
              onChange={(e) => setDate(e.target.value)}
              style={{
                position: "absolute",
                top: 40,
                left: 0,
                opacity: 0,
                width: 0,
                height: 0,
                pointerEvents: "auto",
              }}
            />
          </Grid>

          <Grid item xs={6} sx={{ flex: 1, position: "relative" }}>
            <Typography
              sx={{
                color: "#3B3B3B",
                fontSize: "14px",
                fontWeight: 500,
                mb: 1,
              }}
            >
              Time (IST)
            </Typography>
            <TextField
              fullWidth
              value={time}
              variant="outlined"
              onClick={() => timeInputRef.current?.showPicker()}
              InputProps={{
                readOnly: true,
                startAdornment: (
                  <InputAdornment position="start">
                    <label htmlFor="edit-schedule-time-picker">
                      <AccessTimeIcon
                        sx={{ color: "#1976d2", cursor: "pointer" }}
                      />
                    </label>
                  </InputAdornment>
                ),
              }}
              sx={{
                "& .MuiOutlinedInput-root": {
                  borderRadius: "8px",
                  backgroundColor: "#f5f5f5",
                },
              }}
            />
            <input
              type="time"
              id="edit-schedule-time-picker"
              ref={timeInputRef}
              value={time}
              onChange={(e) => setTime(e.target.value)}
              style={{
                position: "absolute",
                top: 40,
                left: 0,
                opacity: 0,
                width: 0,
                height: 0,
                pointerEvents: "auto",
              }}
            />
          </Grid>
        </Box>
      </DialogContent>

      <DialogActions
        sx={{
          px: 0,
          pt: 3,
          display: "flex",
          justifyContent: "end",
          gap: 2,
        }}
      >
        <Button
          onClick={onClose}
          disabled={loading}
          sx={{
            backgroundColor: "#F4F0FF",
            color: "#0047AB",
            borderRadius: "8px",
            textTransform: "none",
            border: "1px solid #F4F0FF",
            px: 4,
            py: 1,
            fontWeight: 500,
            fontSize: "16px",
            "&:hover": {
              backgroundColor: "#ece9fd",
            },
          }}
        >
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          disabled={!isFormValid || loading}
          sx={{
            backgroundColor: "#0047AB",
            color: "#fff",
            borderRadius: "8px",
            textTransform: "none",
            px: 4,
            py: 1,
            fontWeight: 500,
            fontSize: "16px",
            "&:hover": {
              backgroundColor: "#002f8c",
            },
            "&:disabled": {
              backgroundColor: "#ccc",
              color: "#fff",
            },
          }}
        >
          {loading ? "Saving..." : "Save Changes"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default EditScheduleModal;
