import React, { useState } from "react";
import { Alert, Box, Button, Paper, TextField, Typography } from "@mui/material";
import { useNavigate } from "react-router-dom";
import authService from "../services/authService";
import { Auth } from "../contexts/AuthContext";

const getHomePath = (role = []) =>
  Array.isArray(role) && role.includes("ROLE_ADMIN") ? "/admin" : "/employees/timeline";

const OrganizationOnboarding = () => {
  const navigate = useNavigate();
  const { state, setUser } = Auth();
  const [organizationName, setOrganizationName] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!organizationName.trim()) {
      setError("Organization name is required");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const response = await authService.completeOrganizationOnboarding({
        organizationName: organizationName.trim(),
      });
      const session = authService.extractSession(response);
      setUser({
        ...state,
        ...session,
        role: session.role?.length ? session.role : state.role,
        organizationId: session.organizationId,
        requiresOrganizationSetup: false,
      });
      navigate(getHomePath(session.role?.length ? session.role : state.role), { replace: true });
    } catch (submitError) {
      setError(submitError?.response?.data?.message || submitError?.message || "Failed to create organization");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f4f7fb",
        px: 2,
      }}
    >
      <Paper sx={{ width: "100%", maxWidth: 420, p: 4, borderRadius: 3 }}>
        <Typography variant="h5" sx={{ mb: 1, fontWeight: 700 }}>
          Create your organization
        </Typography>
        <Typography variant="body2" sx={{ mb: 3, color: "#5b6370" }}>
          Finish setup before entering the workspace.
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            label="Organization name"
            value={organizationName}
            onChange={(event) => setOrganizationName(event.target.value)}
            fullWidth
            autoFocus
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            sx={{ mt: 3, height: 44, textTransform: "none" }}
            disabled={loading}
          >
            {loading ? "Setting up..." : "Continue"}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
};

export default OrganizationOnboarding;
