import {
  Avatar,
  Box,
  Button,
  Card,
  Chip,
  IconButton,
  Switch,
  Typography,
  Grid,
  Collapse,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import CloseIcon from "@mui/icons-material/Close";
import { useEffect, useState, useRef, useMemo } from "react";
import SearchIcon from "@mui/icons-material/Search";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import cap from "../../assets/cap.png";
import phone from "../../assets/call.png";
import mail from "../../assets/mail.png";
import insta from "../../assets/insta.png";
import fb from "../../assets/facebook.png";
import twit from "../../assets/twitter.png";
import linkedin from "../../assets/linkedin.png";
import styled from "@emotion/styled";
import InviteUserPopup from "../../components/userMangement/InviteUser";
import {
  appointAsLeader,
  deleteUser,
  reInviteUser,
  toggleAdminRole,
  userActivePlatform,
  userListing,
} from "../../services/adminServices";
import { toast } from "react-toastify";

const LeaderSwitch = styled(Switch)(({ theme }) => ({
  width: 36,
  height: 20,
  padding: 0,
  display: "flex",
  "& .MuiSwitch-switchBase": {
    padding: 2,
    "&.Mui-checked": {
      transform: "translateX(16px)",
      color: "#fff",
      "& + .MuiSwitch-track": {
        backgroundColor: "#2563EB", // Blue
        opacity: 1,
      },
    },
  },
  "& .MuiSwitch-thumb": {
    width: 16,
    height: 16,
    boxShadow: "none",
  },
  "& .MuiSwitch-track": {
    borderRadius: 20 / 2,
    backgroundColor: "#E4E9F2", // Light blue gray
    opacity: 1,
  },
}));

const StatusChip = ({ status }) => {
  switch (status) {
    case "invited":
      return (
        <Chip
          label="Invitation Sent"
          variant="outlined"
          sx={{
            borderRadius: 3,
            color: "#6381AA",
            fontSize: 11,
            border: "1px dashed #8296B2",
          }}
        />
      );
    case "INVITATION_EXPIRED":
      return (
        <Chip
          label="Invitation Expired"
          variant="outlined"
          sx={{
            borderRadius: 3,
            color: "#6381AA",
            fontSize: 13,
            border: "1px dashed #8296B2",
          }}
        />
      );
    case "ACTIVE":
      return (
        <Chip
          label="Active"
          sx={{
            backgroundColor: "#DBFFDF",
            color: "#21AD0F",
            fontSize: "13px",
            fontWeight: "400",
            borderRadius: "16px",
            border: "1px solid #21AD0F",
          }}
        />
      );
    case "Connected":
      return (
        <Chip
          label="Connected"
          sx={{
            backgroundColor: "#DBFFDF",
            color: "#2DA225",
            fontSize: "13px",
            fontWeight: "400",
            borderRadius: "16px",
            border: "1px solid #2DA225",
          }}
        />
      );
    case "Expired":
      return (
        <Chip
          label="Expired"
          sx={{
            backgroundColor: "#FFE5D8",
            color: "#EF8223",
            fontSize: "13px",
            fontWeight: "400",
            borderRadius: "16px",
            border: "1px solid #EF8223",
          }}
        />
      );
    case "Never Connected":
      return (
        <Chip
          label="Never Connected"
          sx={{
            backgroundColor: "#FFEEEF",
            color: "#AA6334",
            fontSize: "13px",
            fontWeight: "400",
            borderRadius: "16px",
            border: "1px solid #AA6334",
          }}
        />
      );
    case "inactive":
      return (
        <Chip
          label="Inactive"
          sx={{
            backgroundColor: "#FED6D6",
            color: "#CC2D1A",
            fontSize: "13px",
            fontWeight: "400",
            borderRadius: "16px",
            border: "1px solid #CC2D1A",
          }}
        />
      );
    default:
      return null;
  }
};

const ActionButton = ({ status, handleInvite, loading }) => {
  switch (status) {
    case "invited":
      return <></>;
    case "INVITATION_EXPIRED":
      return (
        <Button
          sx={{
            bgcolor: "#2D76DC",
            color: "white",
            borderRadius: "8px",
            fontSize: "12px",
            fontWeight: "400",
            padding: "5px 10px",
            textTransform: "capitalize",
            minWidth: "80px",
            marginRight:"45px"
          }}
          onClick={handleInvite}
          disabled={loading}
        >
          {loading ? "Sending..." : "Re-Invite"}
        </Button>
      );
    case "active":
      return (
        <Chip
          label="Disable"
          sx={{
            backgroundColor: "#F4F0FF",
            color: "#CC2D1A",
            fontSize: "14px",
            fontWeight: "400",
            borderRadius: 1,
          }}
        />
      );
    case "inactive":
      return (
        <Chip
          label="Enable"
          sx={{
            backgroundColor: "#F4F0FF",
            color: "#2AC515",
            fontSize: "14px",
            fontWeight: "400",
            borderRadius: 1,
          }}
        />
      );
    default:
      return null;
  }
};

const UserRow = ({
  user,
  expanded,
  onToggle,
  fetchAllUsers,
  userPlatformData,
  onEdit,
  onDelete,
}) => {
  // const [expanded, setExpanded] = useState(false);
  const [open, setOpen] = useState(false);
  const [isLeader, setIsLeader] = useState(user?.isLeader || false);
  const [isAdmin, setIsAdmin] = useState(user?.isAdmin || false);
  const [leaderConfirmOpen, setLeaderConfirmOpen] = useState(false);
  const [pendingLeaderValue, setPendingLeaderValue] = useState(null);
  const [leaderLoading, setLeaderLoading] = useState(false);
  const [reInviteLoading, setReInviteLoading] = useState(false);

  // Sync local state with user prop when data is refreshed
  useEffect(() => {
    setIsLeader(user?.isLeader || false);
    setIsAdmin(user?.isAdmin || false);
  }, [user]);

  // const toggleExpanded = () => {
  //   setExpanded((prev) => !prev);
  // };

  const handleLeaderToggle = (event) => {
    const newValue = event.target.checked;
    setPendingLeaderValue(newValue);
    setLeaderConfirmOpen(true);
  };

  const confirmLeaderToggle = async () => {
    setLeaderLoading(true);
    try {
      setIsLeader(pendingLeaderValue);
      setLeaderConfirmOpen(false);
      const payload = {
        userId: user?.id,
        isLeader: pendingLeaderValue,
      };
      const response = await appointAsLeader(payload);
      if (response?.status === true) {
        toast.success(pendingLeaderValue ? "Appointed as leader" : "Removed as leader");
        fetchAllUsers();
      }
    } catch (error) {
      console.log(error, "error");
      setIsLeader(!pendingLeaderValue);
      toast.error("Failed to update leader status");
    } finally {
      setLeaderLoading(false);
    }
    setPendingLeaderValue(null);
  };

  const cancelLeaderToggle = () => {
    setLeaderConfirmOpen(false);
    setPendingLeaderValue(null);
  };

  const handleAdminToggle = async (event) => {
    const newValue = event.target.checked;
    setIsAdmin(newValue);
    try {
      const payload = {
        userId: user?.id,
        enableAdmin: newValue,
      };
      const response = await toggleAdminRole(payload);
      if (response?.status === "SUCCESS" || response?.status === true) {
        toast.success(newValue ? "Admin role granted" : "Admin role removed");
        fetchAllUsers();
      }
    } catch (error) {
      console.log(error, "error");
      setIsAdmin(!newValue);
      toast.error("Failed to update admin role");
    }
  };

  const handleReInvite = async (data) => {
    setReInviteLoading(true);
    const payload = {
      email: data?.email,
    };
    try {
      const response = await reInviteUser(payload);
      if (response?.status === true) {
        toast.success("Re-invitation sent successfully");
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setReInviteLoading(false);
    }
  };
  const handleClose = () => {
    setOpen(false);
  };
  return (
    <Box
      sx={{
        // py: 2,
        border: "0.6px solid #E0E0E0",
        borderRadius: "12px",
        mb: "10px",
        px: 1,
      }}
    >
      <Grid
        my={1}
        container
        alignItems="center"
        spacing={2}
        sx={{ cursor: "pointer" }}
      >
        <Grid
          item
          xs={12}
          md={3}
          sx={{ display: "flex", alignItems: "center" }}
        >
          <IconButton onClick={onToggle} size="small">
            <ArrowDropDownIcon sx={{ color: "#2D76DC" }} />
          </IconButton>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              // width: 40,
              // height: 40,
              borderRadius: "8px",
            }}
          >
            <Avatar
              src={user?.profilePictureUrl}
              width={"33px"}
              height={"33px"}
              alt="icon"
            />
          </Box>
          <Box onClick={onToggle} ml={1}>
            <Typography
              display={"flex"}
              alignItems={"center"}
              gap={"6px"}
              fontWeight={600}
              fontSize={14}
              color="#000000"
            >
              {user?.name}{" "}
              {user?.isLeader && (
                <img src={cap} width={18} height={18} alt="cap" />
              )}
            </Typography>
            <Typography fontSize={12} fontWeight={400} color="#8E8E8ECC">
              {user?.department}
            </Typography>
          </Box>
        </Grid>
        <Grid
          item
          xs={12}
          md={1.2}
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
          }}
        >
          <Typography fontWeight={400} fontSize={13} color="#95919D">
            Leader
          </Typography>
          <LeaderSwitch checked={isLeader} onChange={handleLeaderToggle} />
        </Grid>

        <Grid
          item
          xs={12}
          md={1.2}
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
          }}
        >
          <Typography fontWeight={400} fontSize={13} color="#95919D">
            Admin
          </Typography>
          <LeaderSwitch checked={isAdmin} onChange={handleAdminToggle} />
        </Grid>

        <Grid onClick={onToggle} item xs={12} md={2.6}>
          <Typography fontSize={12} fontWeight={400} color="#8E8E8ECC">
            <IconButton>
              <img src={phone} width={10} height={8} />
            </IconButton>
            {user?.phone}
          </Typography>
          <Typography fontSize={12} fontWeight={400} color="#8E8E8ECC">
            <IconButton>
              <img width={12} height={11} src={mail} />
            </IconButton>
            {user?.email}
          </Typography>
        </Grid>

        <Grid item xs={12} md={2.5} textAlign={"end"}>
          <StatusChip status={user?.status} />
        </Grid>

        <Grid
          item
          xs={12}
          md={1.5}
          sx={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            gap: 1,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
            <Button
              onClick={() => onEdit(user)}
              sx={{
                color: "#2D76DC",
                bgcolor: "#F4F0FF",
                fontSize: "12px",
                textTransform: "capitalize",
                px: 2,
                py: 0.5,
                borderRadius: "8px",
                minWidth: "80px",
                "&:hover": {
                  bgcolor: "#EDE9FE",
                },
              }}
              size="small"
            >
              Edit
            </Button>
            <IconButton
              onClick={() => onDelete(user)}
              sx={{
                color: "#DA4040",
                bgcolor: "#FED6D6",
                borderRadius: "8px",
                width: 32,
                height: 32,
                "&:hover": {
                  bgcolor: "#FECACA",
                },
              }}
              size="small"
            >
              <DeleteOutlineIcon sx={{ fontSize: "18px" }} />
            </IconButton>
          </Box>
          <ActionButton
            status={user?.status}
            handleInvite={() => handleReInvite(user)}
            loading={reInviteLoading}
          />
        </Grid>
      </Grid>

      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <Box
          sx={{
            backgroundColor: "#F8F8FF",
            mt: 2,
            borderRadius: "8px",
            p: 2,
          }}
        >
          {userPlatformData?.map((platform, i) => (
            <Grid
              key={i}
              container
              spacing={2}
              alignItems="center"
              sx={{
                borderBottom:
                  i < userPlatformData - 1 ? "1px solid #E4E4E4" : "none",
                py: 1,
              }}
            >
              <Grid item xs={12} md={6}>
                <Box display="flex" alignItems="center" gap={1}>
                  <Avatar
                    src={
                      platform?.platform === "FACEBOOK"
                        ? fb
                        : platform?.platform === "LINKEDIN"
                        ? linkedin
                        : platform?.platform === "X"
                        ? twit
                        : insta
                    }
                    sx={{ width: 20, height: 20 }}
                  />
                  <Typography fontSize={12} color="#3C3C3E" fontWeight={400}>
                    {platform?.platform}
                  </Typography>
                </Box>
              </Grid>

              <Grid item xs={12} container textAlign={"end"} md={6}>
                <Grid item textAlign="end" xs={12} md={6} gap={1}>
                  {platform?.status === "CONNECTED" && (
                    <StatusChip status={"Connected"} />
                  )}
                  {platform?.status === "EXPIRED" && (
                    <StatusChip status={"Expired"} />
                  )}
                  {platform?.status === "NEVER_CONNECTED" && (
                    <StatusChip status={"Never Connected"} />
                  )}
                </Grid>
                <Grid item textAlign="end" xs={12} md={6} gap={1}>
                  {["EXPIRED", "NEVER_CONNECTED"].includes(
                    platform?.status
                  ) && (
                    <Button
                      sx={{
                        textTransform: "capitalize",
                        bgcolor: "#2D76DC",
                        fontSize: "11px",
                        color: "#fff",
                        fontWeight: "400",
                        padding: "6px 10px",
                        borderRadius: "8px",
                      }}
                      size="small"
                    >
                      Send Reminder
                    </Button>
                  )}
                </Grid>
              </Grid>
            </Grid>
          ))}
        </Box>
      </Collapse>
      <InviteUserPopup open={open} onClose={handleClose} />

      {/* Leader Role Confirmation Dialog */}
      <Dialog open={leaderConfirmOpen} onClose={cancelLeaderToggle}>
        <DialogTitle>
          {pendingLeaderValue ? "Assign Leader Role" : "Remove Leader Role"}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {pendingLeaderValue
              ? `Are you sure you want to assign ${user?.name} as a Leader?`
              : `Are you sure you want to remove ${user?.name} from the Leader role?`}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelLeaderToggle} color="inherit" disabled={leaderLoading}>
            Cancel
          </Button>
          <Button onClick={confirmLeaderToggle} variant="contained" color="primary" disabled={leaderLoading}>
            {leaderLoading ? "Updating..." : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
const PER_PAGE = 8;
const UserManagement = () => {
  const [open, setOpen] = useState(false);
  const [expandedRowId, setExpandedRowId] = useState(null);
  const [usersData, setUsersData] = useState([]);
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const scrollContainerRef = useRef(null);
  const count = Math.ceil(usersData?.length / PER_PAGE);
  const [userPlatformData, setUserPlatformData] = useState(null);
  const [editUser, setEditUser] = useState(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [deleteUserLoading, setDeleteUserLoading] = useState(false);

  // Sort by ID descending (newest first) and filter by search - memoized
  const filteredUsers = useMemo(() => {
    return [...usersData]
      .sort((a, b) => b.id - a.id)
      .filter((user) => {
        if (!searchQuery.trim()) return true;
        const query = searchQuery.toLowerCase();
        return (
          user?.name?.toLowerCase().includes(query) ||
          user?.email?.toLowerCase().includes(query) ||
          user?.department?.toLowerCase().includes(query)
        );
      });
  }, [usersData, searchQuery]);
  // const paginatedUsers = usersData?.slice(
  //   page * PER_PAGE,
  //   (page + 1) * PER_PAGE
  // );
  const handleClose = () => {
    setOpen(false);
    setEditUser(null);
  };
  const handleInvite = () => {
    setEditUser(null);
    setOpen(true);
  };
  const handleEditUser = (user) => {
    setEditUser(user);
    setOpen(true);
  };

  const handleDeleteClick = (user) => {
    setUserToDelete(user);
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setUserToDelete(null);
    setDeleteDialogOpen(false);
  };

  const handleDeleteConfirm = async () => {
    setDeleteUserLoading(true);
    try {
      const response = await deleteUser(userToDelete.id);
      if (response?.status === true) {
        toast.success("User deleted successfully");
        fetchAllUsers();
      }
    } catch (error) {
      toast.error("Failed to delete user");
      console.log(error);
    } finally {
      setDeleteUserLoading(false);
      setDeleteDialogOpen(false);
      setUserToDelete(null);
    }
  };

  const scrollToTop = () => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const handleInviteSuccess = () => {
    fetchAllUsers();
    scrollToTop();
  };

  const fetchAllUsers = async () => {
    const org_id = sessionStorage.getItem("orgId");
    try {
      const response = await userListing(org_id, page);
      if (response?.status === true) {
        setUsersData(response?.users || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchUserActivePlatform = async (id) => {
    try {
      const response = await userActivePlatform(id);
      console.log(response, "userActivePlatform");
      if (response?.status === true) {
        setUserPlatformData(response?.socialMediaConnectionData || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };

  const onToggle = async (id) => {
    await fetchUserActivePlatform(id);
    setExpandedRowId(expandedRowId === id ? null : id);
  };

  useEffect(() => {
    fetchAllUsers();
  }, [page]);

  return (
    <Card
      elevation={0}
      className="settings-container"
      sx={{ p: 2, borderRadius: 3, height: "100%" }}
    >
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 2,
        }}
      >
        <Typography variant="h6" fontSize={"17px"} fontWeight={500}>
          Users
        </Typography>
        <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
          <TextField
            size="small"
            placeholder="Search users..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon sx={{ color: "#95919D", fontSize: 20 }} />
                </InputAdornment>
              ),
            }}
            sx={{
              width: 220,
              "& .MuiOutlinedInput-root": {
                borderRadius: "8px",
                backgroundColor: "#F8F9FC",
                height: 36,
                fontSize: "13px",
              },
            }}
          />
          <Button
            sx={{
              backgroundColor: "#0047AB",
              color: "white",
              borderRadius: 2,
              fontSize: "12px",
              fontWeight: 500,
              padding: "5px 16px",
              textTransform: "capitalize",
            }}
            onClick={handleInvite}
          >
            + Invite User
          </Button>
        </Box>
      </Box>
      <Box
        ref={scrollContainerRef}
        sx={{ height: "100vh", overflowY: "auto", overflowX: "hidden" }}
        className="scroll-container"
      >
        {filteredUsers?.length > 0 ? (
          filteredUsers?.map((user) => (
            <UserRow
              key={user?.id}
              user={user}
              fetchAllUsers={fetchAllUsers}
              expanded={expandedRowId === user.id}
              // onToggle={() =>
              //   setExpandedRowId(expandedRowId === user.id ? null : user.id)
              // }
              userPlatformData={userPlatformData}
              onToggle={() => onToggle(user.id)}
              onEdit={handleEditUser}
              onDelete={handleDeleteClick}
            />
          ))
        ) : (
          <Box textAlign={"center"}>No users found</Box>
        )}
      </Box>

      {/* <Box
        sx={{
          display: "flex",
          justifyContent: "end",
          mt: 3,
        }}
      >
        <Pagination
          count={count}
          page={page}
          onChange={(e, value) => setPage(value)}
          shape="rounded"
          renderItem={(item) => {
            const isPrev = item.type === "previous";
            const isNext = item.type === "next";
            const isSelected = item.page === page && item.type === "page";

            return (
              <PaginationItem
                {...item}
                sx={{
                  mx: 0.5,
                  borderRadius: 2,
                  width: 40,
                  height: 40,
                  fontWeight: 500,
                  color: isSelected ? "white" : "#6B7280",
                  bgcolor: isSelected ? "#2D76DC !important" : "white",
                  border: isSelected ? "none" : "1px solid #E5E7EB",
                  "&:hover": {
                    backgroundColor: isSelected ? "#b91c1c" : "#F3F4F6",
                  },
                  ...(isPrev && {
                    bgcolor: "#F5F3FF",
                    color: "#1D4ED8",
                    "&:hover": {
                      bgcolor: "#EDE9FE",
                    },
                  }),
                  ...(isNext && {
                    bgcolor: "#F5F3FF",
                    color: "#1D4ED8",
                    "&:hover": {
                      bgcolor: "#EDE9FE",
                    },
                  }),
                }}
                components={{
                  previous: ArrowBackIosNew,
                  next: ArrowForwardIos,
                }}
              />
            );
          }}
        />
      </Box> */}

      <InviteUserPopup
        open={open}
        onClose={handleClose}
        editUser={editUser}
        onSuccess={handleInviteSuccess}
      />

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: "20px",
            padding: "24px",
            position: "relative",
          },
        }}
      >
        <IconButton
          onClick={handleDeleteCancel}
          disabled={deleteUserLoading}
          sx={{ position: "absolute", top: 12, right: 12 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>

        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "24px",
            p: 0,
            mb: 1,
          }}
        >
          Delete User?
        </DialogTitle>

        <DialogContent sx={{ px: 0 }}>
          <Typography
            sx={{
              color: "#95919D",
              fontSize: "16px",
              fontWeight: 400,
              width: "90%",
            }}
          >
            Are you sure you want to delete "{userToDelete?.name}"? This action
            cannot be undone.
          </Typography>
        </DialogContent>

        <DialogActions
          sx={{
            px: 0,
            pt: 4,
            display: "flex",
            justifyContent: "end",
            gap: 2,
          }}
        >
          <Button
            onClick={handleDeleteCancel}
            disabled={deleteUserLoading}
            sx={{
              backgroundColor: "#F4F0FF",
              color: "#0047AB",
              borderRadius: "8px",
              textTransform: "none",
              border: "1px solid #F4F0FF",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#ece9fd",
              },
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleDeleteConfirm}
            disabled={deleteUserLoading}
            sx={{
              backgroundColor: "#DA4040",
              color: "#fff",
              borderRadius: "8px",
              textTransform: "none",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#C53030",
              },
            }}
          >
            {deleteUserLoading ? "Deleting..." : "Delete"}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default UserManagement;
