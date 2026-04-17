import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Auth } from '../contexts/AuthContext';

const PrivateRoute = ({ element }) => {
    const { state, loading } = Auth();
    const location = useLocation();

    if (loading || !state.isAuthenticated || !state.token) {
        return <Navigate to="/sign-in" />;
    }

    if (state.requiresOrganizationSetup && location.pathname !== "/organization-setup") {
        return <Navigate to="/organization-setup" replace />;
    }

    return element;
};

export default PrivateRoute;
