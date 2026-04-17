import { useEffect, useState } from "react";
import { Box, Container, Grid } from "@mui/material";
import Timeline from "../components/Layout/Timeline/Timeline";
import AdminCategories from "../components/Layout/Admin/AdminCategories";
import { useLocation } from "react-router-dom";
import UserManagement from "./admin/UserManagement";
import Settings from "../components/Layout/Settings/Setting";
import VideoManagement from "./admin/VideoManagement";
import ImageManagement from "./admin/ImageManagement";
import AdminTimeline from "./admin/Timeline";
import useScreenSize from "../shared/useScreensize";
import TeamDetails from "../components/Layout/Teams/TeamDetails/Teamsdetails";
import AdminDashboard from "./admin/dashboard";
import ProfileDetails from "./profile";
import AdminTeams from "./AdminTeams";
import Notifications from "../components/Layout/notifications";
import UserService from "../services/categoryService";

const AdminLayout = () => {
  const { isMobileScreen } = useScreenSize();
  const location = useLocation();
  const [teamMemberInfo, setTeamMemberInfo] = useState(null);

  // Fetch team member info when on team-member page
  useEffect(() => {
    const fetchTeamMemberInfo = async () => {
      if (location.pathname.startsWith("/admin/team-member/")) {
        const memberId = location.pathname.split("/").pop();
        try {
          const response = await UserService.getProfile(memberId);
          if (response?.status === true) {
            setTeamMemberInfo(response?.data);
          }
        } catch (error) {
          console.log(error);
        }
      } else {
        setTeamMemberInfo(null);
      }
    };
    fetchTeamMemberInfo();
  }, [location.pathname]);
  const path = location.pathname.split("/").filter(Boolean).pop();
  let pageTitle = "";
  if (path === "admin") {
    pageTitle = "Dashboard";
  } else if (path === "user-management") {
    pageTitle = "User Management";
  } else if (path === "photo-library") {
    pageTitle = "Photo Library";
  } else if (path === "video-library") {
    pageTitle = "Video Library";
  } else if (location.pathname.startsWith("/admin/team-member/")) {
    pageTitle = teamMemberInfo
      ? `${teamMemberInfo.name}${teamMemberInfo.teamName ? ` - ${teamMemberInfo.teamName}` : ''}`
      : "Team Member Profile";
  } else {
    pageTitle = path; // fallback to actual path name
  }
  return (
    <Container
      disableGutters
      maxWidth={false}
      sx={{
        width: "100%",
        overflowX: "hidden",
        backgroundColor: "#F9F5FF",
        minHeight: "100vh",
        padding: "0px 0px 15px 15px",
        boxSizing: "border-box",
      }}
    >
      {isMobileScreen && (
        <Box sx={{ mb: 3, width: "100%", position: "fixed", zIndex: 999 }}>
          <Timeline pageTitle={pageTitle} />
        </Box>
      )}
      <Grid
        container
        spacing={2}
        sx={{
          height: "100%",
          width: "100%",
          maxWidth: "100%",
          overflowX: "hidden",
        }}
      >
        {/* Left Sidebar */}
        <Grid
          item
          sx={{
            overflow: "hidden",
            top: 0,
          }}
          xs={12}
          md={3}
        >
          <Box className="leftSideBar" sx={{ height: "100%" }}>
            <AdminCategories />
            <AdminTeams />
          </Box>
        </Grid>
        <Grid
          item
          xs={12}
          md={9}
          sx={{
            boxSizing: "border-box",
            backgroundColor: "#faf8ff",
          }}
        >
          <Box
            sx={{
              mb: 2,
              width: "100%",
              position: isMobileScreen ? "relative" : "sticky",
              top: 0,
              zIndex: 999,
              backgroundColor: "#fff",
              borderRadius: "16px",
              boxShadow: "0px 2px 8px rgba(0, 0, 0, 0.05)",
            }}
          >
            <Timeline pageTitle={pageTitle} />
          </Box>
          <Box>
            {location.pathname === "/admin/timeline" && (
              <Box
                sx={{
                  // height: isMobileScreen ? "auto" : "calc(140vh - 64px)",
                  height: {
                    xs: "auto",
                    sm: "auto",
                    md: "calc(130vh - 72px)", // slightly reduced to fit 13-inch screens
                  },
                  overflowY: {
                    xs: "visible",
                    sm: "visible",
                    md: "auto",
                  },
                  // height: isMobileScreen ? "auto" : "calc(100vh - 64px)",
                  overflowY: isMobileScreen ? "visible" : "auto",
                  scrollbarWidth: "none", // Firefox
                  msOverflowStyle: "none", // Internet Explorer & Edge
                  "&::-webkit-scrollbar": {
                    display: "none", // Chrome, Safari, Opera
                  },
                }}
              >
                {" "}
                <AdminTimeline />{" "}
              </Box>
            )}
            {location.pathname.startsWith("/admin/team-member/") && (
              <TeamDetails />
            )}
            {location.pathname === "/admin/user-management" && (
              <UserManagement />
            )}
            {location.pathname === "/admin/settings" && <Settings />}
            {location.pathname === "/admin/video-library" && (
              <VideoManagement />
            )}
            {location.pathname === "/admin/profile" && <ProfileDetails />}
            {location.pathname === "/admin" && <AdminDashboard />}
            {location.pathname === "/admin/notifications" && <Notifications />}
            {location.pathname === "/admin/photo-library" && (
              <ImageManagement />
            )}
          </Box>
        </Grid>
      </Grid>
    </Container>
  );
};

export default AdminLayout;
