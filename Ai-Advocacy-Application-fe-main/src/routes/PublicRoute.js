import React from "react";
import { Navigate } from "react-router-dom";
import { Auth } from "../contexts/AuthContext";
import { getStoredRoles } from "../shared/authSession";

const PublicRoute = ({ element }) => {
  const { state } = Auth();

  if (state.isAuthenticated && state.token) {
    const roles = getStoredRoles();
    const isAdmin = roles.includes("ROLE_ADMIN");
    return <Navigate to={isAdmin ? "/admin" : "/employees/timeline"} replace />;
  }

  return element;
};

export default PublicRoute;
