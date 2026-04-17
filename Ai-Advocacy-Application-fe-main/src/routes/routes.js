import React from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import SignUpPage from "../components/Auth/Signup";
import Dashboard from "../components/Dashboard/Dashboard";
import PrivateRoute from "./PrivateRoute";
import PublicRoute from "./PublicRoute";
import TeamDetails from "../components/Layout/Teams/TeamDetails/Teamsdetails";
import Settings from "../components/Layout/Settings/Setting";
import AdminLayout from "../pages/AdminLayout";
import UserManagement from "../pages/admin/UserManagement";
import ImageManagement from "../pages/admin/ImageManagement";
import VideoManagement from "../pages/admin/VideoManagement";
import Timeline from "../pages/admin/Timeline";
import ProfileDetails from "../pages/profile";
import Notifications from "../components/Layout/notifications";
import OrganizationOnboarding from "../pages/OrganizationOnboarding";
import OnboardingRoute from "./OnboardingRoute";

const LegacyDashboardSettingsRedirect = () => {
    const location = useLocation();

    return (
        <Navigate
            to={{
                pathname: "/admin/settings",
                search: location.search,
                hash: location.hash,
            }}
            replace
        />
    );
};

const AppRoutes = () => {
    
    return (
        <Routes>
            {/* Authentication routes — redirect to dashboard if already logged in */}
            <Route path="/" element={<Navigate to="/employees" />} />
            <Route path="/sign-up" element={<PublicRoute element={<SignUpPage formType="signUp" />} />} />
            <Route path="/sign-in" element={<PublicRoute element={<SignUpPage formType="signIn" />} />} />
            <Route path="/password-recovery" element={<PublicRoute element={<SignUpPage formType="passwordRecovery" />} />} />
            <Route path="/update-password" element={<PublicRoute element={<SignUpPage formType="updatePassword" />} />} />
            <Route path="/organization-setup" element={<OnboardingRoute element={<OrganizationOnboarding />} />} />
           

            {/* Dashboard with dynamic rendering */}
            <Route path="/employees" element={<PrivateRoute element={<Dashboard />} />}>
                <Route path="team-member/:id" element={<PrivateRoute element={<TeamDetails />} />} />
                <Route path="profile" element={<PrivateRoute element={<ProfileDetails />} />} />
                <Route path="settings" element={<PrivateRoute element={<Settings />} />} />
                <Route path="timeline" element={<PrivateRoute element={<Dashboard />} />} />
                <Route path="notifications" element={<PrivateRoute element={<Notifications />} />} />
            </Route>
            <Route
                path="/dashboard/settings"
                element={<PrivateRoute element={<LegacyDashboardSettingsRedirect />} />}
            />
            {/* Dashboard with dynamic rendering */}
            <Route path="/admin" element={<PrivateRoute element={<AdminLayout />} />}>
                <Route path="team-member/:id" element={<PrivateRoute element={<TeamDetails />} />} />
                <Route path="profile" element={<PrivateRoute element={<ProfileDetails />} />} />
                <Route path="user-management" element={<PrivateRoute element={<UserManagement />} />} />
                <Route path="photo-library" element={<PrivateRoute element={<ImageManagement />} />} />
                <Route path="video-library" element={<PrivateRoute element={<VideoManagement />} />} />
                <Route path="notifications" element={<PrivateRoute element={<Notifications />} />} />
                <Route path="timeline" element={<PrivateRoute element={<Timeline />} />} />
                <Route path="settings" />
            </Route>
        </Routes>
    );
};

export default AppRoutes;
