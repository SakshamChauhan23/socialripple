import React, { useEffect } from "react";
import { BrowserRouter as Router } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import AppRoutes from "./routes/routes"; // Assuming you have routes set up
import { AuthProvider } from "./contexts/AuthContext";
import authService from "./services/authService";
import ErrorBoundary from "./contexts/errorBoundry";
import { clearStoredSession, updateStoredTokens } from "./shared/authSession";

const theme = createTheme();

function App() {
  useEffect(() => {
    const newToken = async () => {
      try {
        const response = await authService.refreshToken();
        if (response?.status === 200) {
          updateStoredTokens({
            accessToken: response?.data?.accessToken,
            refreshToken: response?.data?.refreshToken,
          });
          console.log("Token refreshed at:", new Date().toLocaleTimeString());
        }
      } catch (error) {
        console.error("Error refreshing token:", error);
        if (error?.response?.status === 401) {
          clearStoredSession();
        }
      }
    };

    const refreshIntervalId = window.setInterval(newToken, 3480000);

    return () => window.clearInterval(refreshIntervalId);
  }, []);

  return (
    <AuthProvider>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Router basename="/">
        <ErrorBoundary>

          <AppRoutes />
        </ErrorBoundary>
        </Router>
      </ThemeProvider>
    </AuthProvider>
  );
}

export default App;
