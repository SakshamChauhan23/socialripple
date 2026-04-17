import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogContent,
  Grid,
  IconButton,
  MenuItem,
  TextField,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import React, { useState, useRef } from "react";
import { inviteUser, updateUser } from "../../services/adminServices";
import { toast } from "react-toastify";
import { uploadMedia } from "../../services/postService";

const InviteUserPopup = ({ open, onClose, editUser = null, onSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    name: "",
    email: "",
    mobileNumber: "",
    role: "",
    department: "",
    jobTitle: "",
  });

  const [errors, setErrors] = useState({});
  const [image, setImage] = useState(null);
  const fileInputRef = useRef(null);
  const [imageIds, setImageIds] = useState([]);

  const isEditMode = !!editUser?.id;

  // Populate form when editUser changes
  React.useEffect(() => {
    if (editUser) {
      setForm({
        name: editUser.name || "",
        email: editUser.email || "",
        mobileNumber: editUser.phone || editUser.mobileNumber || "",
        role: editUser.isAdmin ? "ROLE_ADMIN" : "ROLE_EMPLOYEE",
        department: editUser.department || "",
        jobTitle: editUser.jobTitle || "",
      });
      if (editUser.profilePictureUrl) {
        setImage(editUser.profilePictureUrl);
      }
    } else {
      setForm({
        name: "",
        email: "",
        mobileNumber: "",
        role: "",
        department: "",
        jobTitle: "",
      });
      setImage(null);
    }
  }, [editUser]);

  const validate = () => {
    const newErrors = {};

    if (!form.name.trim()) newErrors.name = "Name is required";
    if (!form.email.trim()) {
      newErrors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      newErrors.email = "Email is invalid";
    }

    if (!form.mobileNumber.trim()) {
      newErrors.mobileNumber = "Mobile number is required";
    } else if (!/^\d{10}$/.test(form.mobileNumber)) {
      newErrors.mobileNumber = "Mobile must be 10 digits";
    }

    if (!form.role) newErrors.role = "Role is required";
    if (!form.department) newErrors.department = "Department is required";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    setErrors({ ...errors, [field]: "" });
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    try {
      setLoading(true);

      if (isEditMode) {
        // Update existing user
        const payload = {
          id: editUser.id,
          name: form.name,
          email: form.email,
          mobileNumber: form.mobileNumber,
          role: [form.role],
          department: form.department,
          jobTitle: form.jobTitle,
          profilePicture: image || editUser.profilePictureUrl || null,
        };
        const response = await updateUser(payload);
        if (response?.status === "SUCCESS" || response?.status === true) {
          toast.success("User updated successfully");
          setForm({
            name: "",
            email: "",
            mobileNumber: "",
            role: "",
            department: "",
            jobTitle: "",
          });
          setImage(null);
          onSuccess?.();
        }
      } else {
        // Invite new user
        const payload = {
          department: form.department,
          email: form?.email,
          mobileNumber: form?.mobileNumber,
          name: form?.name,
          role: [form?.role],
        };
        const response = await inviteUser(payload);
        if (response?.status === true) {
          toast.success("User Invited Successfully");
          setForm({
            name: "",
            email: "",
            mobileNumber: "",
            role: "",
            department: "",
            jobTitle: "",
          });
          onSuccess?.();
          onClose();
        } else {
          toast.error(response?.message || "Failed to invite user");
        }
      }
    } catch (error) {
      console.error(error, "error");
      toast.error(error?.response?.data?.message || error.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (file) => {
    const reader = new FileReader();
    const formData = new FormData();
    formData.append("files", file);
    formData.append("mediaType", "image");
    try {
      const response = await uploadMedia(formData);
      if (response?.status === true) {
        setImageIds(response?.mediaUploadData?.imageIds);
        reader.onload = (e) => setImage(e.target.result);
        reader.readAsDataURL(file);
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
    }
  };

  return (
    <Dialog open={open} elevation={0} onClose={onClose} fullScreen>
      <Box sx={{ backgroundColor: "#f6f3ff", height: "100%" }}>
        {/* Header */}
        <Box
          bgcolor="#fff"
          boxShadow="0px 4px 20px rgba(236,236,236,0.25)"
          display="flex"
          justifyContent="space-between"
          alignItems="center"
          sx={{ pl: 6, pr: 3, pt: 3, pb: 3 }}
        >
          <Typography variant="h6" fontWeight={500} color="#3B3B3B">
            {isEditMode ? "Edit User" : "Invite User"}
          </Typography>
          <IconButton onClick={onClose}>
            <CloseIcon sx={{ color: "#8A8A8A" }} />
          </IconButton>
        </Box>

        {/* Content */}
        <DialogContent sx={{ py: 6, px: 8 }}>
          <Box
            component="form"
            onSubmit={handleSubmit}
            sx={{
              background: "white",
              borderRadius: "12px",
              padding: 4,
              boxShadow: "0px 2px 8px rgba(0,0,0,0.05)",
            }}
          >
            <Grid container spacing={4}>
              <Grid item xs={12} sm={4}>
                <Box
                  onClick={() => fileInputRef.current.click()}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    const file = e.dataTransfer.files[0];
                    if (file && file.type.startsWith("image/")) {
                      handleFileUpload(file);
                    }
                  }}
                  sx={{
                    border: "1px dashed #E5E7EB",
                    height: "280px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    borderRadius: "8px",
                    flexDirection: "column",
                    cursor: "pointer",
                    bgcolor: "#FAFAFA",
                    position: "relative",
                    overflow: "hidden",
                  }}
                >
                  {image ? (
                    <img
                      src={image}
                      alt="Preview"
                      style={{
                        maxWidth: "100%",
                        maxHeight: "100%",
                        objectFit: "contain",
                      }}
                    />
                  ) : (
                    <>
                      <Typography
                        fontSize={32}
                        fontWeight={100}
                        color="#2563EB"
                      >
                        +
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{ color: "#6B7280", mt: 1 }}
                      >
                        Drop your image here, or{" "}
                        <Box
                          component="span"
                          sx={{ color: "#2563EB", fontWeight: 500 }}
                        >
                          browse
                        </Box>
                      </Typography>
                    </>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) handleFileUpload(file);
                    }}
                  />
                </Box>

                {/* <Box
                  onClick={() => fileInputRef.current.click()}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    const file = e.dataTransfer.files[0];
                    if (file && file.type.startsWith("image/")) {
                      setImage(URL.createObjectURL(file));
                    }
                  }}
                  sx={{
                    border: "1px dashed #E5E7EB",
                    height: "280px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    borderRadius: "8px",
                    flexDirection: "column",
                    cursor: "pointer",
                    bgcolor: "#FAFAFA",
                    position: "relative",
                    overflow: "hidden",
                  }}
                >
                  {image ? (
                    <img
                      src={image}
                      alt="Preview"
                      style={{
                        maxWidth: "100%",
                        maxHeight: "100%",
                        objectFit: "contain",
                      }}
                    />
                  ) : (
                    <>
                      <Typography
                        fontSize={32}
                        fontWeight={100}
                        color="#2563EB"
                      >
                        +
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{ color: "#6B7280", mt: 1 }}
                      >
                        Drop your image here, or{" "}
                        <Box
                          component="span"
                          sx={{ color: "#2563EB", fontWeight: 500 }}
                        >
                          browse
                        </Box>
                      </Typography>
                    </>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    hidden
                    accept="image/*"
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) {
                        setImage(URL.createObjectURL(file));
                      }
                    }}
                  />
                </Box> */}
              </Grid>

              {/* Form Fields */}
              <Grid item xs={12} sm={8}>
                <Grid container spacing={2}>
                  <Grid item xs={12} mb={2} md={6}>
                    <TextField
                      fullWidth
                      value={form.name}
                      onChange={handleChange("name")}
                      error={!!errors.name}
                      helperText={errors.name}
                      variant="standard"
                      InputProps={{
                        disableUnderline: false,
                        sx: {
                          pl: 1.5,
                          fontSize: 18,
                          color: "#111827", // Input text color
                          "&::placeholder": {
                            color: "#D1D5DB", // Tailwind's gray-300
                            opacity: 1,
                          },
                        },
                      }}
                      InputLabelProps={{
                        // shrink: true,
                        sx: {
                          color: "#E0E0E0",
                          fontWeight: 500,
                          fontSize: 18,
                        },
                      }}
                      label="Name"
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    />
                  </Grid>
                </Grid>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={6} mb={2}>
                    <TextField
                      label="Enter Email"
                      fullWidth
                      type="email"
                      value={form.email}
                      onChange={handleChange("email")}
                      error={!!errors.email}
                      helperText={errors.email}
                      variant="standard"
                      InputProps={{
                        disableUnderline: false,
                        sx: {
                          pl: 1.5,
                          fontSize: 18,
                          color: "#111827", // Input text color
                          "&::placeholder": {
                            color: "#D1D5DB", // Tailwind's gray-300
                            opacity: 1,
                          },
                        },
                      }}
                      InputLabelProps={{
                        // shrink: true,
                        sx: {
                          color: "#E0E0E0",
                          fontWeight: 500,
                          fontSize: 18,
                        },
                      }}
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    />
                  </Grid>
                  <Grid item xs={12} md={6} mb={2}>
                    <TextField
                      select
                      label="Role"
                      fullWidth
                      value={form.role}
                      onChange={handleChange("role")}
                      error={!!errors.role}
                      helperText={errors.role}
                      variant="standard"
                      InputLabelProps={{
                        sx: {
                          color: "#ccc",
                          fontSize: "16px",
                        },
                      }}
                      SelectProps={{
                        IconComponent: () => (
                          <svg
                            width="30px"
                            height="25px"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                          >
                            <path d="M7 10l5 5 5-5H7z" fill="#2D76DC" />
                          </svg>
                        ),
                        sx: {
                          paddingTop: "4px",
                        },
                      }}
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    >
                      <MenuItem value="ROLE_EMPLOYEE">EMPLOYEE</MenuItem>
                      <MenuItem value="ROLE_ADMIN">ADMIN</MenuItem>
                    </TextField>
                  </Grid>

                  <Grid item xs={12} md={6} mb={2}>
                    <TextField
                      label="Mobile Number"
                      fullWidth
                      type="tel"
                      value={form.mobileNumber}
                      onChange={handleChange("mobileNumber")}
                      error={!!errors.mobileNumber}
                      helperText={errors.mobileNumber}
                      variant="standard"
                      InputProps={{
                        disableUnderline: false,
                        sx: {
                          pl: 1.5,
                          fontSize: 18,
                          color: "#111827", // Input text color
                          "&::placeholder": {
                            color: "#D1D5DB", // Tailwind's gray-300
                            opacity: 1,
                          },
                        },
                      }}
                      InputLabelProps={{
                        // shrink: true,
                        sx: {
                          color: "#E0E0E0",
                          fontWeight: 500,
                          fontSize: 18,
                        },
                      }}
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    />
                  </Grid>

                  <Grid item xs={12} mb={2} md={6}>
                    <TextField
                      label="Job Title"
                      fullWidth
                      value={form.jobTitle}
                      onChange={handleChange("jobTitle")}
                      variant="standard"
                      placeholder="e.g. Software Engineer"
                      InputProps={{
                        disableUnderline: false,
                        sx: {
                          pl: 1.5,
                          fontSize: 18,
                          color: "#111827",
                          "&::placeholder": {
                            color: "#D1D5DB",
                            opacity: 1,
                          },
                        },
                      }}
                      InputLabelProps={{
                        sx: {
                          color: "#E0E0E0",
                          fontWeight: 500,
                          fontSize: 18,
                        },
                      }}
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    />
                  </Grid>

                  <Grid item xs={12} mb={2} md={6}>
                    <TextField
                      select
                      label="Department"
                      fullWidth
                      value={form.department}
                      onChange={handleChange("department")}
                      error={!!errors.department}
                      helperText={errors.department}
                      variant="standard"
                      InputLabelProps={{
                        sx: {
                          color: "#ccc",
                          fontSize: "16px",
                        },
                      }}
                      SelectProps={{
                        IconComponent: () => (
                          <svg
                            width="30px"
                            height="25px"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                          >
                            <path d="M7 10l5 5 5-5H7z" fill="#2D76DC" />
                          </svg>
                        ),
                        sx: {
                          paddingTop: "4px",
                        },
                      }}
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    >
                      <MenuItem value="HR">HR</MenuItem>
                      <MenuItem value="IT">IT</MenuItem>
                      <MenuItem value="MARKETING">Marketing</MenuItem>
                    </TextField>
                  </Grid>
                  <Box
                    mt={3}
                    display="flex"
                    width={"100%"}
                    justifyContent="flex-end"
                    gap={2}
                  >
                    <Button
                      onClick={onClose}
                      disabled={loading}
                      sx={{
                        bgcolor: "#F4F0FF",
                        color: "#0047AB",
                        border: "none",
                        fontSize: "14px",
                        fontWeight: "500",
                        textTransform: "none",
                        px: 4,
                        "&:hover": {
                          bgcolor: "#EDE9FE",
                        },
                      }}
                    >
                      Cancel
                    </Button>
                    {loading ? (
                      <CircularProgress sx={{ ml: 2 }} />
                    ) : (
                      <Button
                        type="submit"
                        sx={{
                          bgcolor: "#0047AB",
                          color: "#fff",
                          fontSize: "14px",
                          fontWeight: "500",
                          textTransform: "none",
                          px: 4,
                          "&:hover": {
                            bgcolor: "#1E40AF",
                          },
                        }}
                      >
                        {isEditMode ? "Update User" : "Send Invite"}
                      </Button>
                    )}
                  </Box>
                </Grid>
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
      </Box>
    </Dialog>
  );
};

export default InviteUserPopup;
