import React, { useState, useEffect, useRef } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
  Typography,
  TextField,
  Box,
  InputAdornment,
  Grid,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import AccessTimeIcon from "@mui/icons-material/AccessTime";

const RescheduleModal = ({ open, onClose, scheduleData, onSave }) => {
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [loading, setLoading] = useState(false);
  const dateInputRef = useRef(null);
  const timeInputRef = useRef(null);

  useEffect(() => {
    if (scheduleData?.scheduledTimeUtc) {
      const utcDate = new Date(scheduleData.scheduledTimeUtc);
      // Convert to IST for display
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
  }, [scheduleData]);

  const handleSave = async () => {
    if (!date || !time) {
      return;
    }

    // Combine date + time into ISO format
    const combinedDateTime = new Date(`${date}T${time}:00Z`);

    setLoading(true);
    try {
      await onSave({
        ...scheduleData,
        scheduledTimeUtc: combinedDateTime.toISOString().split(".")[0] + "Z",
      });
    } finally {
      setLoading(false);
    }
  };

  // Get minimum date (today in IST)
  const getMinDate = () => {
    const now = new Date();
    return now.toLocaleDateString("en-CA", {
      timeZone: "Asia/Kolkata",
    });
  };

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
        Reschedule Post
      </DialogTitle>

      <DialogContent sx={{ px: 0, pt: 2 }}>
        <Typography
          sx={{
            color: "#95919D",
            fontSize: "14px",
            fontWeight: 400,
            mb: 3,
          }}
        >
          Select a new date and time for this scheduled post
        </Typography>

        <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
          <Grid item xs={12} sx={{ position: "relative" }}>
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
                    <label htmlFor="reschedule-date-picker">
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
              id="reschedule-date-picker"
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

          <Grid item xs={12} sx={{ position: "relative" }}>
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
                    <label htmlFor="reschedule-time-picker">
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
              id="reschedule-time-picker"
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
          disabled={!date || !time || loading}
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
          {loading ? "Saving..." : "Save"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RescheduleModal;
