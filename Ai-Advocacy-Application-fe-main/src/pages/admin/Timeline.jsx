import { Box } from "@mui/material";
import { Grid } from "@mui/system";
// import PostCards from "../../components/Layout/Postcards/Postcards";
import { Auth } from "../../contexts/AuthContext";
import UserService from "../../services/categoryService";
import React, { Suspense, useEffect } from "react";
import PostCreation from "../../components/Layout/Postcreation/Postcreation";
import useScreenSize from "../../shared/useScreensize";
const PostCards = React.lazy(() =>
  import("../../components/Layout/Postcards/Postcards")
);
const AdminTimeline = () => {
  const {
    userProfile,
    setUserProfile,
    loyaltyPoints,
    setLoyaltyPoints,
    searchValue,
    setSearchValue,
  } = Auth();

  const { isMobileScreen } = useScreenSize();
  const fetchProfile = async () => {
    try {
      const response = await UserService?.getProfile();
      if (response?.status === true) {
        setUserProfile(response?.data);
      }
    } catch (error) {
      console.log(error);
    } finally {
    }
  };
  const fetchLoyaltyPoints = async () => {
    try {
      const response = await UserService?.getLoyaltyPoints();
      if (response?.status === true) {
        setLoyaltyPoints(response?.loyalty);
      }
    } catch (error) {
      console.log(error);
    } finally {
    }
  };

  useEffect(() => {
    fetchLoyaltyPoints();
    fetchProfile();
  }, []);
  return (
    <div className="section">
      <Grid
        item
        xs={12}
        md={12}
        className={!isMobileScreen ? "postContentContainer" : ""}
        sx={{ width: "100%", minHeight: "100%" }}
      >
        <Box>
          <PostCreation userProfile={userProfile} />
        </Box>
        <Box>
          <Suspense fallback={<div>Loading Data...</div>}>
            <PostCards />
          </Suspense>
        </Box>
      </Grid>
      {/* <div className="reset-password"></div> */}
    </div>
  );
};

export default AdminTimeline;
