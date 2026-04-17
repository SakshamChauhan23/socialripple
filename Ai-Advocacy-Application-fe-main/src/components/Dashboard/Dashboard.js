import React, { useCallback, useEffect, useState } from "react";
import { Grid, Container, Box } from "@mui/material";
import Categories from "../Layout/Categories/Categories";
import Teams from "../Layout/Teams/Teams";
import Timeline from "../Layout/Timeline/Timeline";
import PostCreation from "../Layout/Postcreation/Postcreation";
import PostCards from "../Layout/Postcards/Postcards";
import Settings from "../Layout/Settings/Setting";
import TeamDetails from "../Layout/Teams/TeamDetails/Teamsdetails";
import UserDetails from "../Layout/Userdetails/Userdetails";
import ActivitiesAndTopPosts from "../Layout/ActivitiesAndTopPosts/Activitiesandtopposts";
import ProfileDetails from "../../pages/profile";
import useScreenSize from "../../shared/useScreensize";
import { useLocation } from "react-router-dom";
import UserService from "../../services/categoryService";
import { Auth } from "../../contexts/AuthContext";
import Notifications from "../Layout/notifications";
import "./Dashboard.css";

const Dashboard = () => {
  const { isMobileScreen } = useScreenSize();
  const [activeView, setActiveView] = useState("category-teams");
  const path = useLocation();
  const { userProfile, setUserProfile, setLoyaltyPoints } = Auth();

  const isEmployeeFeedRoute =
    path.pathname === "/employees" || path.pathname === "/employees/timeline";
  const forceMainContentOnMobile = !isEmployeeFeedRoute;

  const handleViewChange = (view) => {
    if (activeView === view) {
      if (view === "category-teams") {
        setActiveView("postContent");
      } else if (view === "postContent") {
        setActiveView("category-teams");
      }
    } else {
      setActiveView(view);
    }
  };

  const getPageTitle = () => {
    if (path.pathname === "/employees") {
      return "DashBoard";
    }
    if (path.pathname === "/employees/timeline") {
      return "Timeline";
    }
    if (path.pathname === "/employees/settings") {
      return "Settings";
    }
    if (path.pathname === "/employees/notifications") {
      return "Notifications";
    }
    if (path.pathname === "/employees/profile") {
      return "Profile";
    }
    return path.pathname.split("/").filter(Boolean).pop();
  };

  const fetchProfile = useCallback(async () => {
    try {
      const response = await UserService?.getProfile();
      if (response?.status === true) {
        setUserProfile(response?.data);
      }
    } catch (error) {
      console.log(error);
    }
  }, [setUserProfile]);

  const fetchLoyaltyPoints = useCallback(async () => {
    try {
      const response = await UserService?.getLoyaltyPoints();
      if (response?.status === true) {
        setLoyaltyPoints(response?.loyalty);
      }
    } catch (error) {
      console.log(error);
    }
  }, [setLoyaltyPoints]);

  useEffect(() => {
    fetchLoyaltyPoints();
    fetchProfile();
  }, [fetchLoyaltyPoints, fetchProfile]);

  const renderEmployeeContent = () => {
    if (path.pathname === "/employees/settings") {
      return <Settings />;
    }

    if (path.pathname === "/employees/profile") {
      return <ProfileDetails />;
    }

    if (path.pathname === "/employees/notifications") {
      return <Notifications />;
    }

    if (path.pathname.startsWith("/employees/team-member/")) {
      const memberId = path.pathname.split("/").pop();
      return (
        <Box>
          <TeamDetails memberId={memberId} />
        </Box>
      );
    }

    return (
      <>
        <Box>
          <PostCreation userProfile={userProfile} />
        </Box>
        <Box>
          <PostCards />
        </Box>
      </>
    );
  };

  return (
    <Container
      disableGutters
      maxWidth={false}
      sx={{
        width: "100vw",
        overflowX: "hidden",
        backgroundColor: "#F9F5FF",
        minHeight: "100vh",
        padding: "0px 20px 15px 15px",
        boxSizing: "border-box",
      }}
    >
      {isMobileScreen && (
        <Box sx={{ mb: 3, width: "100%", position: "fixed", zIndex: 999 }}>
          <Timeline
            pageTitle={getPageTitle()}
            onClickChangeView={handleViewChange}
            userProfile={userProfile}
          />
        </Box>
      )}

      {isMobileScreen ? (
        <Grid container spacing={3} sx={{ height: "100%" }}>
          {!forceMainContentOnMobile && activeView === "category-teams" && (
            <Grid item xs={12} md={3} sx={{ height: "100%" }}>
              <Box>
                <Categories />
                <Teams />
              </Box>
            </Grid>
          )}

          {(forceMainContentOnMobile || activeView === "postContent") && (
            <Grid item xs={12} md={8}>
              {renderEmployeeContent()}
            </Grid>
          )}

          {!forceMainContentOnMobile && activeView === "userDetails" && (
            <Grid
              item
              xs={12}
              md={4}
              className={!isMobileScreen && "postContentContainer"}
            >
              <Box>
                <UserDetails />
                <ActivitiesAndTopPosts />
              </Box>
            </Grid>
          )}
        </Grid>
      ) : (
        <Grid container spacing={2} sx={{ height: "100%" }}>
          <Grid item xs={12} md={3} sx={{ height: "100%" }}>
            <Box className={!isMobileScreen && "leftSideBar"}>
              <Categories />
              <Teams />
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
              <Timeline pageTitle={getPageTitle()} userProfile={userProfile} />
            </Box>
            <Grid container spacing={3}>
              <Grid
                item
                xs={12}
                md={12}
                sx={{
                  height: {
                    xs: "auto",
                    sm: "auto",
                    md: "calc(130vh - 72px)",
                  },
                  overflowY: {
                    xs: "visible",
                    sm: "visible",
                    md: "auto",
                  },
                  scrollbarWidth: "none",
                  msOverflowStyle: "none",
                  "&::-webkit-scrollbar": {
                    display: "none",
                  },
                }}
                className={!isMobileScreen ? "postContentContainer" : ""}
              >
                {renderEmployeeContent()}
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      )}
    </Container>
  );
};

export default Dashboard;
