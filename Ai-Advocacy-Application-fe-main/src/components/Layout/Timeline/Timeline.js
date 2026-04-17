import React, { useState, useEffect, useRef } from "react";
import {
  Box,
  TextField,
  IconButton,
  Badge,
  InputAdornment,
  Divider,
  Avatar,
  Typography,
  Popover,
  Snackbar,
  Alert,
  CircularProgress,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  Button,
} from "@mui/material";
import PersonOutlineIcon from "@mui/icons-material/PersonOutline";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import LogoutIcon from "@mui/icons-material/Logout";
import CloseIcon from "@mui/icons-material/Close";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import hamburger from "../../../assets/hamburger.svg";
import "./Timeline.css";
import useScreenSize from "../../../shared/useScreensize";
import { useLocation, useNavigate } from "react-router-dom";
import { getAllNotifications } from "../../../services/adminServices";
import UserService from "../../../services/categoryService";
import { Auth } from "../../../contexts/AuthContext";
import useWebSocket from "./useWebSocket";
import { formatNotification } from "../../../shared/notificationFormatter";
import { getStoredRoles } from "../../../shared/authSession";
import { userApiBase, userWsBase } from "../../../services/runtimeConfig";

const buildWebSocketUrl = (token) => {
  if (!token) return null;
  const wsPath = process.env.REACT_APP_WS_PATH || "/ws";

  const explicitWsBase = userWsBase;
  if (explicitWsBase) {
    const wsBase = explicitWsBase.endsWith("/")
      ? explicitWsBase.slice(0, -1)
      : explicitWsBase;
    return `${wsBase}${wsPath}?token=${encodeURIComponent(token)}`;
  }

  try {
    const apiBase = userApiBase || window.location.origin;
    const parsedUrl = new URL(apiBase, window.location.origin);
    const wsProtocol = parsedUrl.protocol === "https:" ? "wss:" : "ws:";
    return `${wsProtocol}//${parsedUrl.host}${wsPath}?token=${encodeURIComponent(token)}`;
  } catch (error) {
    const wsProtocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${wsProtocol}//${window.location.host}${wsPath}?token=${encodeURIComponent(token)}`;
  }
};

const getBasePath = (roles = []) =>
  Array.isArray(roles) && roles.includes("ROLE_ADMIN") ? "/admin" : "/employees";

const Timeline = ({ onClickChangeView, pageTitle }) => {
  const { state, setUserProfile, searchValue, setSearchValue } = Auth();
  const location = useLocation();
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(100);
  const [notificationsList, setNotificationsList] = useState([]);
  const [userProfile, setUserProfiles] = useState(null);
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarNotification, setSnackbarNotification] = useState(null);
  const [profileAnchorEl, setProfileAnchorEl] = useState(null);
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };
  const { isMobileScreen } = useScreenSize();
  const navigate = useNavigate();
  const roleList =
    Array.isArray(state?.role) && state.role.length > 0 ? state.role : getStoredRoles();
  const basePath = getBasePath(roleList);

  const handleProfileMenuOpen = (event) => {
    setProfileAnchorEl(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setProfileAnchorEl(null);
  };

  const handleViewProfile = () => {
    handleProfileMenuClose();
    navigate(`${basePath}/profile`);
  };

  const handleSettings = () => {
    handleProfileMenuClose();
    navigate(`${basePath}/settings`);
  };

  const handleLogoutClick = () => {
    handleProfileMenuClose();
    setLogoutDialogOpen(true);
  };

  const confirmLogout = () => {
    sessionStorage.clear();
    window.location.href = "/sign-in";
  };

  const fetchAllNotifications = async () => {
    try {
      const response = await getAllNotifications(page, limit);
      if (response?.status === true) {
        setNotificationsList(response?.notificationData || []);
      }
    } catch (error) {
    } finally {
    }
  };
  const fetchProfile = async () => {
    try {
      const response = await UserService?.getProfile();
      if (response?.status === true) {
        setUserProfiles(response?.data);
        setUserProfile(response?.data);
      }
    } catch (error) {
      console.log(error);
    } finally {
    }
  };

  useEffect(() => {
    fetchProfile();
    fetchAllNotifications();
  }, []);

  const handleNavigate = () => {
    navigate(`${basePath}/notifications`);
    handleClose();
  };

  const getTimeAgo = (date) => {
    const seconds = Math.floor((Date.now() - new Date(date)) / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (seconds < 60) return `${seconds}s ago`;
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  };
  const userToken = sessionStorage.getItem("authToken");

  useWebSocket(buildWebSocketUrl(userToken), (data) => {
    const formattedNotification = formatNotification(data);

    setSnackbarNotification(formattedNotification);
    setOpenSnackbar(true);
  });
  const [tempValue, setTempValue] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const searchRef = useRef(null);

  // update tempValue as user types
  const onChange = (e) => {
    setTempValue(e.target.value);
  };

  // Trigger search on Enter key
  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      setSearchValue(tempValue);
    }
  };

  // Clear search
  const handleClearSearch = () => {
    setTempValue("");
    setSearchValue("");
  };

  useEffect(() => {
    if (tempValue) {
      setIsSearching(true);
    }
    const delayDebounce = setTimeout(() => {
      setSearchValue(tempValue);
      setIsSearching(false);
    }, 1000);

    return () => clearTimeout(delayDebounce);
  }, [tempValue, setSearchValue]);

  // Clear search when path changes
  useEffect(() => {
    setTempValue("");
    setSearchValue("");
  }, [location.pathname]);

  // Clear search when clicking outside search field
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchRef.current && !searchRef.current.contains(event.target) && tempValue) {
        setTempValue("");
        setSearchValue("");
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [tempValue, setSearchValue]);

  return (
    <Box className="timeLineBar" sx={{ bgcolor: "#fff" }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          // flexWrap: { xs: "wrap", md: "nowrap" },
          padding: "0px 16px",
          alignItems: "center",
        }}
      >
        {/* Title */}
        {!isMobileScreen ? (
          <Typography
            // variant="subtitle1"
            variant="h6"
            // sx={{ fontSize: 24, fontWeight: 500 }}
            style={{ textTransform: "capitalize" }}
            // className="timeline-header"
          >
            {pageTitle}
          </Typography>
        ) : (
          <Box className="timeline-header humberger">
            <img
              // onClick={() => onClickChangeView("postContent")}
              src={hamburger}
              alt="hamBurger"
              style={{
                width: 20,
                height: 20,
                marginRight: 8,
                transform: "translateY(4px)",
              }}
            />
            {pageTitle}
          </Box>
        )}
        {/* Right Section (Search + Icons) */}
        <Box className="menuIcons">
          {(location?.pathname === "/employees" ||
            location?.pathname === "/admin/timeline") && (
            <>
              {isMobileScreen ? (
                <IconButton>
                  <SearchIcon color="action" style={{ color: "#2D76DC" }} />
                </IconButton>
              ) : (
                <TextField
                  ref={searchRef}
                  placeholder="Search"
                  size="small"
                  variant="outlined"
                  sx={{
                    background: "#F4F6F9",
                    borderRadius: "20px",
                    width: "100%",
                    maxWidth: "300px",
                    "& fieldset": { border: "none" },
                    "& .MuiOutlinedInput-root": {
                      borderRadius: "20px",
                      fontSize: "14px",
                    },
                  }}
                  value={tempValue}
                  onChange={onChange}
                  onKeyDown={handleKeyDown}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon
                          color="action"
                          style={{ color: "#2D76DC" }}
                        />
                      </InputAdornment>
                    ),
                    endAdornment: tempValue && (
                      <InputAdornment position="end">
                        {isSearching ? (
                          <CircularProgress size={18} />
                        ) : (
                          <IconButton
                            size="small"
                            onClick={handleClearSearch}
                            sx={{ padding: "4px" }}
                          >
                            <ClearIcon style={{ color: "#999", fontSize: "18px" }} />
                          </IconButton>
                        )}
                      </InputAdornment>
                    ),
                  }}
                />
                // <TextField
                //   placeholder="Search"
                //   size="small"
                //   variant="outlined"
                //   sx={{
                //     background: "#F4F6F9",
                //     borderRadius: "20px",
                //     width: "100%",
                //     maxWidth: "300px",
                //     "& fieldset": { border: "none" },
                //     "& .MuiOutlinedInput-root": {
                //       borderRadius: "20px",
                //       fontSize: "14px",
                //     },
                //   }}
                //   value={searchValue}
                //   onChange={onChange}
                //   InputProps={{
                //     startAdornment: (
                //       <InputAdornment position="start">
                //         <SearchIcon
                //           color="action"
                //           style={{ color: "#2D76DC" }}
                //         />
                //       </InputAdornment>
                //     ),
                //   }}
                // />
              )}
            </>
          )}

          <Divider orientation="vertical" flexItem className="icon-devider" />
          <IconButton onClick={handleClick}>
            <Badge badgeContent={notificationsList?.length} color="success">
              <svg
                width="19"
                height="21"
                viewBox="0 0 19 21"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M7.5 19H11.5C11.5 20.1 10.6 21 9.5 21C8.4 21 7.5 20.1 7.5 19ZM18.5 17V18H0.5V17L2.5 15V9C2.5 5.9 4.5 3.2 7.5 2.3V2C7.5 0.9 8.4 0 9.5 0C10.6 0 11.5 0.9 11.5 2V2.3C14.5 3.2 16.5 5.9 16.5 9V15L18.5 17ZM14.5 9C14.5 6.2 12.3 4 9.5 4C6.7 4 4.5 6.2 4.5 9V16H14.5V9Z"
                  fill="#2D76DC"
                />
              </svg>
            </Badge>
          </IconButton>
          <Popover
            elevation={0}
            open={open}
            anchorEl={anchorEl}
            onClose={handleClose}
            anchorOrigin={{
              vertical: "bottom",
              horizontal: "right",
            }}
            transformOrigin={{
              vertical: "top",
              horizontal: "right",
            }}
            PaperProps={{
              sx: {
                borderRadius: 3,
                width: 400,
                maxHeight: 500,
                p: 1,
                boxShadow: "0px 4px 12px 0px rgba(165, 165, 165, 0.25)",
              },
            }}
          >
            <Box
              sx={{
                display: "flex",
                gap: 2,
                alignItems: "center",
                p: 1,
              }}
            >
              <Typography
                variant="h6"
                fontSize={{ md: "16px" }}
                fontWeight={500}
              >
                Notifications
              </Typography>
              <Box
                sx={{
                  backgroundColor: "#5BA40C",
                  color: "white",
                  borderRadius: "50%",
                  width: 20,
                  height: 20,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "10px",
                  fontWeight: 500,
                }}
              >
                {notificationsList?.length}
              </Box>
            </Box>
            <Box
              className="scroll-container"
              sx={{ maxHeight: 350, overflowY: "auto" }}
            >
              {notificationsList?.map((item, index) => {
                const formattedNotification = formatNotification(item);

                return (
                <Box
                  key={item.id || `${item?.createdAt || "notification"}-${index}`}
                  sx={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: 2,
                    backgroundColor: "#F8F6FD",
                    borderRadius: 2,
                    p: 1.5,
                    mb: 1,
                    zIndex: 1000,
                  }}
                >
                  <Avatar
                    sx={{
                      mt: 1,
                      width: 25,
                      height: 25,
                    }}
                    // src={item.icon}
                  />
                  <Box flex={1}>
                    <Typography
                      variant="subtitle1"
                      color="#3B3B3B"
                      fontWeight={600}
                      fontSize={{ xs: "12px", sm: "14px", md: "12px" }}
                    >
                      {formattedNotification.title}
                    </Typography>
                    <Typography
                      variant="body2"
                      fontSize={{ xs: "12px", sm: "14px", md: "10px" }}
                      color="#999999"
                    >
                      {formattedNotification.message}
                    </Typography>
                  </Box>
                  <Typography
                    variant="caption"
                    fontSize={{ xs: "12px", sm: "14px", md: "10px" }}
                    color="#B5B5B5"
                  >
                    {item?.createdAt ? getTimeAgo(item?.createdAt) : ""}
                  </Typography>
                </Box>
              )})}
            </Box>

            <Box textAlign="center" mt={1} pb={1}>
              <Typography
                onClick={handleNavigate}
                sx={{
                  fontWeight: 500,
                  color: "#4160FC",
                  cursor: "pointer",
                  fontSize: { xs: "12px", sm: "12px", md: "12px" },
                }}
              >
                View all
              </Typography>
            </Box>
          </Popover>
          <Divider orientation="vertical" flexItem className="icon-devider" />
          <IconButton onClick={handleProfileMenuOpen}>
            {userProfile?.profilePicture ? (
              <Avatar
                src={userProfile?.profilePicture}
                alt={userProfile?.name}
              />
            ) : (
              <Avatar>{userProfile?.name}</Avatar>
            )}
          </IconButton>
          <Menu
            anchorEl={profileAnchorEl}
            open={Boolean(profileAnchorEl)}
            onClose={handleProfileMenuClose}
            anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
            transformOrigin={{ vertical: "top", horizontal: "right" }}
            PaperProps={{
              sx: {
                borderRadius: 2,
                minWidth: 160,
                boxShadow: "0px 4px 12px 0px rgba(165, 165, 165, 0.25)",
                mt: 1,
              },
            }}
          >
            <MenuItem onClick={handleViewProfile}>
              <PersonOutlineIcon sx={{ mr: 1, color: "#2D76DC" }} fontSize="small" />
              View Profile
            </MenuItem>
            <MenuItem onClick={handleSettings}>
              <SettingsOutlinedIcon sx={{ mr: 1, color: "#2D76DC" }} fontSize="small" />
              Settings
            </MenuItem>
            <MenuItem onClick={handleLogoutClick} sx={{ color: "#D93A3A" }}>
              <LogoutIcon sx={{ mr: 1, color: "#D93A3A" }} fontSize="small" />
              Logout
            </MenuItem>
          </Menu>
        </Box>
      </Box>
      <Snackbar
        open={openSnackbar}
        autoHideDuration={6000}
        onClose={() => setOpenSnackbar(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert severity="info" onClose={() => setOpenSnackbar(false)}>
          {snackbarNotification?.title ? `${snackbarNotification.title}: ` : ""}
          {snackbarNotification?.message || "You have a new update."}
        </Alert>
      </Snackbar>

      <Dialog
        open={logoutDialogOpen}
        onClose={() => setLogoutDialogOpen(false)}
        PaperProps={{
          sx: {
            borderRadius: "16px",
            padding: "20px",
            maxWidth: "420px",
            width: "100%",
          },
        }}
      >
        <IconButton
          onClick={() => setLogoutDialogOpen(false)}
          sx={{ position: "absolute", top: 16, right: 16 }}
        >
          <CloseIcon />
        </IconButton>
        <DialogTitle
          sx={{ fontWeight: "500", color: "#484848", fontSize: "20px", p: 0, mb: 1 }}
        >
          Are You Sure You Want To Log Out?
        </DialogTitle>
        <DialogContent sx={{ padding: "20px 5px" }}>
          <Typography
            sx={{ fontSize: "16px", color: "#616161", mb: 3, textAlign: "start" }}
          >
            You will need to sign in again to access your account.
          </Typography>
          <Box display="flex" justifyContent="end" gap={2}>
            <Button
              onClick={() => setLogoutDialogOpen(false)}
              sx={{
                backgroundColor: "#F4F0FF",
                color: "#0D47A1",
                textTransform: "none",
                fontSize: "16px",
                fontWeight: 500,
                borderRadius: "8px",
                px: 4,
                py: 1,
                "&:hover": { backgroundColor: "#E9D5FF" },
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={confirmLogout}
              sx={{
                backgroundColor: "#D93A3A",
                color: "#fff",
                textTransform: "none",
                fontSize: "16px",
                fontWeight: 500,
                borderRadius: "8px",
                px: 4,
                py: 1,
                "&:hover": { backgroundColor: "#b71c1c" },
              }}
            >
              Confirm
            </Button>
          </Box>
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default Timeline;
