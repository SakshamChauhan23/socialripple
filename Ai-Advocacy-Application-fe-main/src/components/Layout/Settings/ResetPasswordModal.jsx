import React, { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  IconButton,
  InputAdornment,
  Typography,
  CircularProgress,
} from "@mui/material";
import { Visibility, VisibilityOff, Close } from "@mui/icons-material";
import authService from "../../../services/authService";
import { toast } from "react-toastify";

const ResetPasswordModal = ({ open, onClose }) => {
  const [form, setForm] = useState({
    current: "",
    new: "",
    confirm: "",
  });
  const [loader, setLoader] = useState(false);

  const [showPassword, setShowPassword] = useState({
    current: false,
    new: false,
    confirm: false,
  });

  const [errors, setErrors] = useState({});

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    setErrors({ ...errors, [field]: "" });
  };

  const toggleVisibility = (field) => () => {
    setShowPassword({ ...showPassword, [field]: !showPassword[field] });
  };

  const validate = () => {
    const newErrors = {};
    if (!form.current) newErrors.current = "Current password is required";
    if (!form.new) newErrors.new = "New password is required";
    if (!form.confirm) newErrors.confirm = "Please confirm new password";
    if (form.new === form.current)
      newErrors.new = "New password must be different from current";
    if (form.new && form.confirm && form.new !== form.confirm)
      newErrors.confirm = "Passwords do not match";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (validate()) {
      const payload = {
        currentPassword: form.current,
        newPassword: form.new,
        confirmNewPassword: form.confirm,
      };
      setLoader(true);
      try {
        const response = await authService.updatePassword(payload);
        if (response?.status === 200) {
          toast.success("Password Updated Successfully");
          onClose();
        }
      } catch (error) {
      } finally {
        setLoader(false);
      }
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          borderRadius: 4,
          p: 3,
          width: 400,
        },
      }}
    >
      <DialogTitle sx={{ fontWeight: 600, fontSize: 22 }}>
        Reset Password
        <IconButton
          onClick={onClose}
          sx={{ position: "absolute", right: 16, top: 16 }}
        >
          <Close />
        </IconButton>
      </DialogTitle>

      <DialogContent>
        <Typography mt={2} variant="subtitle2" color="#928C9B" mb={0.8}>
          Current Password
        </Typography>
        <TextField
          fullWidth
          variant="standard"
          placeholder="Enter Current Password"
          type={showPassword.current ? "text" : "password"}
          value={form.current}
          onChange={handleChange("current")}
          error={!!errors.current}
          helperText={errors.current}
          sx={{ mb: 2 }}
          InputProps={{
            endAdornment: (
              <InputAdornment sx={{ marginRight: "15px" }} position="end">
                <IconButton onClick={toggleVisibility("current")} edge="end">
                  {showPassword.current ? (
                    <VisibilityOff sx={{ color: "#2D76DC" }} />
                  ) : (
                    <Visibility sx={{ color: "#2D76DC" }} />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        <Typography mt={2} variant="subtitle2" color="#928C9B" mb={0.8}>
          New Password
        </Typography>
        <TextField
          fullWidth
          placeholder="Enter New Password"
          type={showPassword.new ? "text" : "password"}
          value={form.new}
          onChange={handleChange("new")}
          variant="standard"
          error={!!errors.new}
          helperText={errors.new}
          sx={{ mb: 2 }}
          InputProps={{
            endAdornment: (
              <InputAdornment sx={{ marginRight: "15px" }} position="end">
                <IconButton onClick={toggleVisibility("new")} edge="end">
                  {showPassword.new ? (
                    <VisibilityOff sx={{ color: "#2D76DC" }} />
                  ) : (
                    <Visibility sx={{ color: "#2D76DC" }} />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        <Typography mt={2} variant="subtitle2" color="#928C9B" mb={0.8}>
          Confirm Password
        </Typography>
        <TextField
          fullWidth
          placeholder="Re-enter New Password"
          type={showPassword.confirm ? "text" : "password"}
          value={form.confirm}
          onChange={handleChange("confirm")}
          variant="standard"
          error={!!errors.confirm}
          helperText={errors.confirm}
          sx={{ mb: 1 }}
          InputProps={{
            endAdornment: (
              <InputAdornment sx={{ marginRight: "15px" }} position="end">
                <IconButton onClick={toggleVisibility("confirm")} edge="end">
                  {showPassword.confirm ? (
                    <VisibilityOff sx={{ color: "#2D76DC" }} />
                  ) : (
                    <Visibility sx={{ color: "#2D76DC" }} />
                  )}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 3, pt: 2 }}>
        <Button
          onClick={onClose}
          disabled={loader}
          variant="outlined"
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
        {loader ? (
          <CircularProgress sx={{ ml: 2 }} />
        ) : (
          <Button
            onClick={handleSubmit}
            disabled={loader}
            variant="contained"
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
          >
            {loader ? "Updating..." : "Confirm"}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ResetPasswordModal;
