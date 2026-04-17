import React from "react";
import { Navigate } from "react-router-dom";
import { Auth } from "../contexts/AuthContext";

const getHomePath = (role = []) =>
  Array.isArray(role) && role.includes("ROLE_ADMIN") ? "/admin" : "/employees/timeline";

const OnboardingRoute = ({ element }) => {
  const { state, loading } = Auth();

  if (loading || !state.isAuthenticated || !state.token) {
    return <Navigate to="/sign-in" replace />;
  }

  if (!state.requiresOrganizationSetup) {
    return <Navigate to={getHomePath(state.role)} replace />;
  }

  return element;
};

export default OnboardingRoute;
