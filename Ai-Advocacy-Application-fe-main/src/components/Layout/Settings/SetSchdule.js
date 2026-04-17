import React, { useRef, useState } from "react";
import {
  Box,
  Grid,
  Typography,
  TextField,
  Button,
  Checkbox,
  FormControlLabel,
  IconButton,
  Select,
  MenuItem,
  Dialog,
  DialogContent,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import InputAdornment from "@mui/material/InputAdornment";
const SchedulePostModal = ({ open, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [startDate, setStartDate] = useState("2025-05-12");
  const [endDate, setEndDate] = useState("2025-05-22");
  const [startTime, setStartTime] = useState({
    hour: "09", // will be split to '0' and '9'
    minute: "00", // will be split to '0' and '0'
    ampm: "AM",
  });

  const [endTime, setEndTime] = useState({
    hour: "06",
    minute: "30",
    ampm: "PM",
  });

  const [platforms, setPlatforms] = useState({
    all: true,
    instagram: false,
    facebook: false,
    linkedin: false,
    x: false,
  });

  const handlePlatformChange = (name) => (e) => {
    const checked = e.target.checked;
    if (name === "all") {
      setPlatforms({
        all: checked,
        instagram: false,
        facebook: false,
        linkedin: false,
        x: false,
      });
    } else {
      setPlatforms({ ...platforms, [name]: checked, all: false });
    }
  };

  const formatDate = (dateStr) => {
    const [year, month, day] = dateStr.split("-");
    return `${day}/${month}/${year}`;
  };

  const handleConfirm = async () => {
    const payload = {
      startDate,
      endDate,
      startTime,
      endTime,
      platforms,
    };
    setLoading(true);
    try {
      console.log("Scheduled Post:", payload);
      onClose();
    } finally {
      setLoading(false);
    }
  };

  const dateInputRef = useRef(null);
  const dateInputRef2 = useRef(null);

  return (
    <Dialog open={open} onClose={() => onClose()}>
      <DialogContent>
        {/* Close button */}
        <IconButton
          sx={{ position: "absolute", top: 16, right: 16 }}
          onClick={() => onClose()}
        >
          <CloseIcon />
        </IconButton>

        <Typography variant="h6" fontWeight={600} mb={3}>
          Schedule Posts
        </Typography>

        <Grid container spacing={2}>
          {/* Start Date */}

          <Grid item xs={6} sx={{ padding: "30px", position: "relative" }}>
            <Typography variant="body2" color="#95919D" mb={1}>
              Start Date
            </Typography>

            <TextField
              fullWidth
              value={startDate}
              variant="standard"
              onClick={() => dateInputRef.current?.showPicker()}
              InputProps={{
                readOnly: true,
                startAdornment: (
                  <InputAdornment position="start">
                    <label htmlFor="start-date-picker">
                      <CalendarMonthOutlinedIcon
                        sx={{ color: "#1976d2", cursor: "pointer" }}
                      />
                    </label>
                  </InputAdornment>
                ),
              }}
            />

            <input
              type="date"
              id="start-date-picker"
              ref={dateInputRef}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              style={{
                position: "absolute",
                top: 50,
                left: 0,
                opacity: 0,
                width: 0,
                height: 0,
                pointerEvents: "auto",
              }}
            />
          </Grid>

          <Grid item xs={6} sx={{ padding: "30px", position: "relative" }}>
            <Typography variant="body2" color="#95919D" mb={1}>
              End Date
            </Typography>

            <TextField
              fullWidth
              value={endDate}
              variant="standard"
              onClick={() => dateInputRef2.current?.showPicker()}
              InputProps={{
                readOnly: true,
                startAdornment: (
                  <InputAdornment position="start">
                    <label htmlFor="start-date-picker">
                      <CalendarMonthOutlinedIcon
                        sx={{ color: "#1976d2", cursor: "pointer" }}
                      />
                    </label>
                  </InputAdornment>
                ),
              }}
            />

            <input
              type="date"
              id="start-date-picker"
              ref={dateInputRef2}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              style={{
                position: "absolute",
                top: 50,
                left: 0,
                opacity: 0,
                width: 0,
                height: 0,
                pointerEvents: "auto",
              }}
            />
          </Grid>

          {/* Start Time */}
          <Grid item xs={5.8}>
            <Typography variant="body2" color="#95919D" mb={1}>
              Start Time
            </Typography>
            <Box display="flex" alignItems="center" gap={1}>
              <TextField
                value={startTime.hour[0]}
                onChange={(e) =>
                  setStartTime({
                    ...startTime,
                    hour: e.target.value + startTime.hour[1],
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <TextField
                value={startTime.hour[1]}
                onChange={(e) =>
                  setStartTime({
                    ...startTime,
                    hour: startTime.hour[0] + e.target.value,
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <Typography padding={0}>:</Typography>
              <TextField
                value={startTime.minute[0]}
                onChange={(e) =>
                  setStartTime({
                    ...startTime,
                    minute: e.target.value + startTime.minute[1],
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <TextField
                value={startTime.minute[1]}
                onChange={(e) =>
                  setStartTime({
                    ...startTime,
                    minute: startTime.minute[0] + e.target.value,
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <Select
                value={startTime.ampm}
                onChange={(e) =>
                  setStartTime({ ...startTime, ampm: e.target.value })
                }
                sx={{
                  width: 72,
                  height: 38,
                  "& .MuiSelect-icon": { color: "#2D76DC", width: "20px" },
                }}
              >
                <MenuItem value="AM">AM</MenuItem>
                <MenuItem value="PM">PM</MenuItem>
              </Select>
            </Box>
          </Grid>

          {/* End Time */}
          <Grid item xs={5.8}>
            <Typography variant="body2" color="#95919D" mb={1}>
              End Time
            </Typography>
            <Box display="flex" alignItems="center" gap={1}>
              <TextField
                value={endTime.hour[0]}
                onChange={(e) =>
                  setEndTime({
                    ...endTime,
                    hour: e.target.value + endTime.hour[1],
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <TextField
                value={endTime.hour[1]}
                onChange={(e) =>
                  setEndTime({
                    ...endTime,
                    hour: endTime.hour[0] + e.target.value,
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <Typography padding={0}>:</Typography>
              <TextField
                value={endTime.minute[0]}
                onChange={(e) =>
                  setEndTime({
                    ...endTime,
                    minute: e.target.value + endTime.minute[1],
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <TextField
                value={endTime.minute[1]}
                onChange={(e) =>
                  setEndTime({
                    ...endTime,
                    minute: endTime.minute[0] + e.target.value,
                  })
                }
                inputProps={{
                  maxLength: 1,
                  inputMode: "numeric",
                  style: { textAlign: "center" },
                }}
                sx={{
                  width: 45,
                  "& .MuiInputBase-input": { padding: "7px 5px !important" },
                }}
              />
              <Select
                value={endTime.ampm}
                onChange={(e) =>
                  setEndTime({ ...endTime, ampm: e.target.value })
                }
                sx={{
                  width: 72,
                  height: 38,
                  "& .MuiSelect-icon": { color: "#2D76DC", width: "20px" },
                }}
              >
                <MenuItem value="AM">AM</MenuItem>
                <MenuItem value="PM">PM</MenuItem>
              </Select>
            </Box>
          </Grid>

          {/* Platforms */}
          {/* Platforms */}
          <Grid item xs={12} mt={4}>
            <Typography variant="body2" color="#95919D" mb={1}>
              Post to
            </Typography>
            <Box display="flex" flexWrap="wrap" >
              <FormControlLabel
                control={
                  <Checkbox
                    checked={platforms.all}
                    onChange={handlePlatformChange("all")}
                    sx={{
                      color: "#1976d2",
                      "&.Mui-checked": {
                        color: "#1976d2",
                      },
                    }}
                  />
                }
                label={
                  <strong style={{ color: platforms.all ? "#000" : "#333" }}>
                    All
                  </strong>
                }
              />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={platforms.instagram}
                    onChange={handlePlatformChange("instagram")}
                  />
                }
                label={
                  <span style={{ color: "#000", fontWeight: 500 }}>
                    Instagram
                  </span>
                }
              />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={platforms.facebook}
                    onChange={handlePlatformChange("facebook")}
                    disabled
                  />
                }
                label={
                  <span style={{ color: "#c4c4c4", fontWeight: 400 }}>
                    Facebook
                  </span>
                }
              />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={platforms.linkedin}
                    onChange={handlePlatformChange("linkedin")}
                  />
                }
                label={
                  <span style={{ color: "#000", fontWeight: 500 }}>
                    Linkedin
                  </span>
                }
              />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={platforms.x}
                    onChange={handlePlatformChange("x")}
                  />
                }
                label={
                  <span style={{ color: "#000", fontWeight: 500 }}>X</span>
                }
              />
            </Box>
          </Grid>
        </Grid>

        {/* Buttons */}
        <Box mt={4} display="flex" justifyContent="flex-end" gap={2}>
          <Button
            variant="outlined"
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
            variant="contained"
            disabled={loading}
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
            }}
            onClick={handleConfirm}
          >
            {loading ? "Scheduling..." : "Confirm"}
          </Button>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default SchedulePostModal;
