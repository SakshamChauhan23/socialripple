import React, { useEffect, useRef, useState } from "react";
import {
  Card,
  Typography,
  Box,
  List,
  ListItem,
  ListItemText,
  Collapse,
  IconButton,
  Dialog,
  DialogContent,
  DialogTitle,
  Button,
  Menu,
  MenuItem,
  Grid,
  TextField,
  DialogActions,
  ListItemSecondaryAction,
  ListItemAvatar,
  Avatar,
  Checkbox,
} from "@mui/material";
import { grey } from "@mui/material/colors";
import "../components/Layout/Teams/Teams.css";
import Position1 from "../assets/Position-1.svg";
import Position2 from "../assets/Position-2.svg";
import Position3 from "../assets/Position-3.svg";
import coins from "../assets/coins.svg";
import { useNavigate, useParams } from "react-router-dom";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import MoreVertIcon from "@mui/icons-material/MoreVert";
import CloseIcon from "@mui/icons-material/Close";
import {
  addTeamMember,
  createNewTeam,
  deleteMemberFromTeam,
  deleteTeam,
  getAllLeaders,
  getAllTeams,
  getTeamDetails,
  updateTeam,
} from "../services/teamServices";
import { toast } from "react-toastify";
import { userListing } from "../services/adminServices";
const svgIcons = [Position1, Position2, Position3];

const AdminTeams = () => {
  const { id } = useParams();
  const [activeTeam, setActiveTeam] = useState(id);
  const [openTeamId, setOpenTeamId] = useState(null);
  const [activeTab, setActiveTab] = useState("Leaderboard");
  const [openAddTeam, setOpenAddTeam] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const menu = Boolean(anchorEl);
  const [discard, setDiscard] = useState(false);
  const [Teams, setTeams] = useState([]);
  const [TeamDetails, setTeamDetails] = useState([]);
  const [openAddMember, setOpenAddMember] = useState(false);
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [selected, setSelected] = useState([]);
  const [search, setSearch] = useState("");
  const [user, setUser] = useState(null);
  const [usersData, setUsersData] = useState([]);
  const [loader, setLoader] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [leaderBoard,setLeaderBoard] = useState([]);
  const handleToggle = (email) => {
    setSelected((prev) =>
      prev.includes(email) ? prev.filter((e) => e !== email) : [...prev, email]
    );
  };

  const onConfirm = async () => {
    setSelectedMembers(selected);
    try {
      const payload = {
        userIds: selected,
      };
      setLoader(true);
      const response = await addTeamMember(payload, team?.teamId);
      if (response?.status === true) {
        toast.success("Member(s) added successfully");
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setOpenAddMember(false);
      setLoader(false);
      setSelected([]);
    }
  };

  const filteredMembers = usersData?.filter((m) =>
    m?.name?.toLowerCase().includes(search.toLowerCase())
  );
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: "",
  });

  const [errors, setErrors] = useState({});
  const [image, setImage] = useState(null);
  const fileInputRef = useRef(null);
  // const leaderboard = [
  //   { name: "testing", points: 9500 },
  //   { name: "Manu George", points: 5555 },
  //   { name: "Stephen George", points: 4078 },
  //   { name: "Clara Mike", points: 4500 },
  //   { name: "Clara Mike", points: 4500 },
  //   { name: "Clara Mike", points: 4500 },
  //   { name: "Clara Mike", points: 4500 },
  //   { name: "Binu Sathyan", points: 4000 },
  // ];

  const toggleSection = async (team) => {
    await fetchTeamsDetails(team?.teamId);

    setOpenTeamId((prev) => (prev === team.teamId ? null : team.teamId));
  };
  const fetchTeamsDetails = async (id) => {
    try {
      const response = await getTeamDetails(id);
      if (response?.status === true) {
        setTeamDetails(response);
      }
    } catch (error) {
      console.error("Error fetching teams:", error);
    }
  };
  const handleMemberClick = (data) => {
    navigate(`/admin/team-member/${data?.userId}`);
    setActiveTeam(data?.userId);
  };

  const fetchAllTeams = async () => {
    try {
      const response = await getAllTeams();
      if (response?.status === true) {
        setTeams(response?.teams);
      }
    } catch (error) {
      console.error("Error fetching teams:", error);
    }
  };
  const fetchAllLeaders = async () => {
    try {
      const response = await getAllLeaders();
      if (response?.status === true) {
        setLeaderBoard(response?.leaderList);
      }
    } catch (error) {
      console.error("Error fetching teams:", error);
    }
  };

  useEffect(() => {
    fetchAllTeams();
    fetchAllLeaders();
  }, []);
  const [team, setTeam] = useState({});

  const handleEditTeam = (event, team) => {
    setAnchorEl(event.currentTarget);
    setOpenTeamId(null);
    setTeam(team);
  };
  const [type, setType] = useState("");
  const handleEditTeamData = (type) => {
    setOpenAddTeam(true);
    setType(type);
    if (type === "Add") {
      setForm({ name: "" });
      setImage(null);
    } else {
      setForm({ name: team?.teamName });
      setImage(team?.imageUrl);
    }
  };
  const handleClose = () => {
    setAnchorEl(null);
  };
  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    setErrors({ ...errors, [field]: "" });
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name) {
      setErrors({ name: "Team name is required" });
      return;
    }
    setLoader(true);
    try {
      const payload = {
        teamName: form.name,
        imageUrl: image,
      };
      if (type === "Edit") {
        const newPayload = {
          newTeamName: form.name,
          imageUrl: image,
        };
        const response = await updateTeam(newPayload, team?.teamId);
        if (response?.status === true) {
          setForm({ name: "" });
          setImage(null);
          toast.success("Successfully Updated");
        }
      } else {
        const response = await createNewTeam(payload);
        if (response?.status === true) {
          setForm({ name: "" });
          setImage(null);
          toast.success("Team Created Successfully");
        }
      }
    } catch (error) {
    } finally {
      setLoader(false);
      setOpenAddTeam(false);
      fetchAllTeams();
    }
  };
  const closeModal = () => {
    setOpenAddTeam(false);
  };
  const onClose = () => {
    setOpenAddTeam(false);
  };

  const handleConfirm = async () => {
    setDeleteLoading(true);
    try {
      if (type === "member") {
        const payload = {
          teamId: team?.teamId,
          userId: user?.userId,
        };
        const response = await deleteMemberFromTeam(payload);
        if (response?.status === true) {
          toast.success("removed from team");
          fetchAllTeams();
        }
      } else {
        const response = await deleteTeam(team?.teamId);
        if (response?.status === true) {
          toast.success("Deleted");
          fetchAllTeams();
        }
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setDeleteLoading(false);
      setDiscard(false);
    }
  };
  const handleAddMember = async (id) => {
    await fetchAllUsers();
    setOpenAddMember(true);
  };

  const handleDelete = (type, data) => {
    setDiscard(true);
    setType(type);
    setUser(data);
  };

  const fetchAllUsers = async () => {
    const org_id = sessionStorage.getItem("orgId");
    try {
      const response = await userListing(org_id);
      if (response?.status === true) {
        setUsersData(response?.users || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };

  return (
    <Card className="teams-container">
      {/* Header */}
      <Box
        display={"flex"}
        justifyContent={"space-between"}
        alignItems={"center"}
      >
        <Typography color="#3B3B3B" className="teams-header">
          Teams
        </Typography>
        <IconButton onClick={() => handleEditTeamData("Add")}>
          <AddIcon sx={{ color: "#2D76DC", fontSize: "25px" }} />
        </IconButton>
      </Box>

      {/* Tabs */}
      <Box
        display="flex"
        justifyContent="space-between"
        // sx={{
        //   mb: 2,
        //   mt: 2,
        // }}
      >
        <Box
          onClick={() => setActiveTab("Leaderboard")}
          sx={{
            flex: 1,
            textAlign: "center",
            cursor: "pointer",
            padding: "8px 0",
            fontSize: "14px",
            fontWeight: activeTab === "Leaderboard" ? 500 : 400,
            color: activeTab === "Leaderboard" ? "#3B3B3B" : "#95919D",
            borderBottom:
              activeTab === "Leaderboard" ? `2px solid #2D76DC` : "none",
          }}
        >
          Leaderboard
        </Box>
        <Box
          onClick={() => setActiveTab("All Teams")}
          sx={{
            flex: 1,
            textAlign: "center",
            cursor: "pointer",
            padding: "8px 0",
            fontSize: "14px",
            fontWeight: activeTab === "All Teams" ? 500 : 400,
            color: activeTab === "All Teams" ? "#3B3B3B" : "#95919D",
            borderBottom:
              activeTab === "All Teams" ? `2px solid #2D76DC` : "none",
          }}
        >
          All Teams
        </Box>
      </Box>

      {/* Leaderboard Tab */}
      {activeTab === "Leaderboard" && (
        <List
          sx={{ height: "41vh", overflowY: "auto" }}
          className="scroll-container"
        >
          {leaderBoard?.map((team, index) => (
            <ListItem
              key={index}
              // onClick={() => handleMemberClick(team)}
              sx={{
                display: "flex",
                alignItems: "center",
                // cursor: "pointer",
                justifyContent: "space-between",
                borderBottom: "1px solid #EEEEEE",
              }}
            >
              {/* Custom SVG for top 3 */}
              {index < 3 ? (
                <img
                  src={svgIcons[index]}
                  alt={`Rank ${index + 1}`}
                  style={{
                    width: 18,
                    height: 18,
                    marginRight: 3,
                    // marginLeft: -4,
                  }}
                />
              ) : (
                /* Numbering for remaining items (starting from 4) */
                <Box sx={{ fontWeight: 400, mr: 2 }}>{index + 1}</Box>
              )}

              {/* Player Name */}
              <ListItemText
                primaryTypographyProps={{
                  fontSize: "13px", // directly applies to inner Typography
                }}
                primary={team.userName}
              />

              {/* Player Points */}
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  color: "#8296B2",
                  fontWeight: 400,
                  fontFamily: "Poppins",
                  fontSize: "13px",
                }}
              >
                <img
                  src={coins}
                  alt="coins"
                  style={{
                    width: 18,
                    height: 18,
                    marginRight: 6,
                  }}
                />
                {team.totalPoints}
              </Box>
            </ListItem>
          ))}
        </List>
      )}

      {/* All Teams Tab */}
      {activeTab === "All Teams" && (
        <Box
          sx={{ height: "43vh", overflowY: "auto" }}
          className="scroll-container"
        >
          <List>
            {Teams?.map((team, index) => (
              <React.Fragment key={index}>
                <ListItem
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "8px 16px",
                    cursor: "pointer",
                  }}
                >
                  <Box
                    width={"85%"}
                    onClick={() => toggleSection(team)}
                    display="flex"
                    alignItems="center"
                  >
                    <Avatar
                      src={team?.imageUrl || ""}
                      alt="teams"
                      style={{
                        width: 18,
                        height: 18,
                        marginRight: 4,
                      }}
                    />
                    <ListItemText
                      primaryTypographyProps={{
                        fontSize: "13px", // directly applies to inner Typography
                      }}
                      primary={team?.teamName}
                    />
                  </Box>
                  <Box display={"flex"} alignItems={"center"}>
                    {openTeamId === team.teamId ? (
                      <svg
                        onClick={() => toggleSection(team)}
                        width="11"
                        height="5"
                        viewBox="0 0 11 5"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path d="M5.5 0L11 5H0L5.5 0Z" fill="#2D76DC" />
                      </svg>
                    ) : (
                      <svg
                        onClick={() => toggleSection(team)}
                        width="11"
                        height="6"
                        viewBox="0 0 11 6"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path d="M5.5 5.5L11 0.5H0L5.5 5.5Z" fill="#2D76DC" />
                      </svg>
                    )}
                    <div>
                      <IconButton
                        onClick={(event) => handleEditTeam(event, team)}
                      >
                        <MoreVertIcon sx={{ color: "#8296B2" }} />
                      </IconButton>
                      <Menu
                        anchorEl={anchorEl}
                        open={menu}
                        onClose={handleClose}
                        PaperProps={{
                          sx: {
                            left: "200px",
                            borderRadius: "12px",
                            paddingY: 1,
                            boxShadow: "0px 4px 20px rgba(199, 199, 199, 0.1)",
                            minWidth: 180,
                          },
                        }}
                      >
                        {/* Add Member */}
                        <MenuItem
                          onClick={() => {
                            handleAddMember();
                            handleClose();
                          }}
                          sx={{
                            display: "flex",
                            gap: 1.5,
                            fontWeight: 500,
                            color: "#2563EB", // Blue
                          }}
                        >
                          <AddIcon fontSize="small" sx={{ color: "#2563EB" }} />
                          Add Member
                        </MenuItem>

                        {/* Edit */}
                        <MenuItem
                          onClick={() => {
                            handleEditTeamData("Edit");
                            handleClose();
                          }}
                          sx={{
                            display: "flex",
                            gap: 1.5,
                            fontWeight: 500,
                            color: "#2563EB", // Blue
                          }}
                        >
                          <EditIcon
                            fontSize="small"
                            sx={{ color: "#2563EB" }}
                          />
                          Edit
                        </MenuItem>

                        {/* Delete */}
                        <MenuItem
                          onClick={() => {
                            handleDelete("team");
                            handleClose();
                          }}
                          sx={{
                            display: "flex",
                            gap: 1.5,
                            fontWeight: 500,
                            color: "#DC2626", // Red
                          }}
                        >
                          <DeleteIcon
                            fontSize="small"
                            sx={{ color: "#DC2626" }}
                          />
                          Delete
                        </MenuItem>
                      </Menu>
                    </div>
                  </Box>
                </ListItem>
                <Collapse
                  in={openTeamId === team?.teamId}
                  // in={expandedSections[team.teamId]}
                  timeout="auto"
                  unmountOnExit
                >
                  <List component="div" disablePadding sx={{ pl: 4 }}>
                    {TeamDetails?.members?.length > 0 ? (
                      TeamDetails?.members?.map((member, idx) => (
                        <ListItem
                          key={idx}
                          sx={{
                            borderRadius: "8px",
                            "&:hover": {
                              backgroundColor: "#E1E8F2",
                              cursor: "pointer",
                            },
                            backgroundColor:
                              Number(activeTeam) === Number(member.userId)
                                ? "#E1E8F2"
                                : "transparent",
                          }}
                          onClick={() => handleMemberClick(member)}
                        >
                          <></>
                          <Box
                            sx={{
                              width: 8,
                              height: 8,
                              backgroundColor:
                                Number(activeTeam) === Number(member.userId)
                                  ? "#2D76DC"
                                  : "#D9D9D9",
                              borderRadius: "50%",
                              marginRight: 2,
                            }}
                          />
                          <ListItemText
                            primaryTypographyProps={{
                              fontSize: "13px", // directly applies to inner Typography
                            }}
                            primary={member?.name}
                          />
                          <IconButton
                            onClick={() => handleDelete("member", member)}
                          >
                            <DeleteIcon
                              fontSize="small"
                              sx={{ color: "#DC2626" }}
                            />
                          </IconButton>
                        </ListItem>
                      ))
                    ) : (
                      <ListItem>
                        <ListItemText
                          primary="No members yet"
                          sx={{ color: grey[500] }}
                          primaryTypographyProps={{
                            fontSize: "13px", // directly applies to inner Typography
                          }}
                        />
                      </ListItem>
                    )}
                  </List>
                </Collapse>
              </React.Fragment>
            ))}
          </List>
        </Box>
      )}
      <Dialog open={openAddTeam} elevation={0} onClose={closeModal}>
        <Box>
          {/* Header */}
          <Box
            bgcolor="#fff"
            boxShadow="0px 4px 20px rgba(236,236,236,0.25)"
            display="flex"
            justifyContent="space-between"
            alignItems="center"
            sx={{ pl: 6, pr: 3, pt: 3, pb: 3 }}
          >
            <Typography variant="h6" fontWeight={500} color="#3B3B3B">
              {type} Team
            </Typography>
            <IconButton onClick={onClose}>
              <CloseIcon sx={{ color: "#8A8A8A" }} />
            </IconButton>
          </Box>

          {/* Content */}
          <DialogContent>
            <Box
              component="form"
              onSubmit={handleSubmit}
              sx={{
                background: "white",
                borderRadius: "12px",
              }}
            >
              <Grid container spacing={4}>
                <Grid item xs={12} sm={12}>
                  <Box
                    onClick={() => fileInputRef.current.click()}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault();
                      const file = e.dataTransfer.files[0];
                      if (file && file.type.startsWith("image/")) {
                        setImage(URL.createObjectURL(file));
                      }
                    }}
                    sx={{
                      border: "1px dashed #E5E7EB",
                      height: "150px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      borderRadius: "8px",
                      flexDirection: "column",
                      cursor: "pointer",
                      bgcolor: "#FAFAFA",
                      position: "relative",
                      overflow: "hidden",
                    }}
                  >
                    {image ? (
                      <img
                        src={image}
                        alt="Preview"
                        style={{
                          maxWidth: "100%",
                          maxHeight: "100%",
                          objectFit: "contain",
                        }}
                      />
                    ) : (
                      <>
                        <Typography
                          fontSize={32}
                          fontWeight={100}
                          color="#2563EB"
                        >
                          +
                        </Typography>
                        <Typography
                          variant="body2"
                          sx={{ color: "#6B7280", mt: 1 }}
                        >
                          Drop your image here, or{" "}
                          <Box
                            component="span"
                            sx={{ color: "#2563EB", fontWeight: 500 }}
                          >
                            browse
                          </Box>
                        </Typography>
                      </>
                    )}
                    <input
                      ref={fileInputRef}
                      type="file"
                      hidden
                      accept="image/*"
                      onChange={(e) => {
                        const file = e.target.files[0];
                        if (file) {
                          setImage(URL.createObjectURL(file));
                        }
                      }}
                    />
                  </Box>
                </Grid>

                {/* Form Fields */}
                <Grid item xs={12} sm={12}>
                  <Grid item xs={12} mb={2} md={12}>
                    <TextField
                      fullWidth
                      value={form.name}
                      onChange={handleChange("name")}
                      error={!!errors.name}
                      helperText={errors.name}
                      variant="standard"
                      InputProps={{
                        disableUnderline: false,
                        sx: {
                          pl: 1.5,
                          fontSize: 18,
                          color: "#111827", // Input text color
                          "&::placeholder": {
                            color: "#D1D5DB", // Tailwind's gray-300
                            opacity: 1,
                          },
                        },
                      }}
                      InputLabelProps={{
                        // shrink: true,
                        sx: {
                          color: "#E0E0E0",
                          fontWeight: 500,
                          fontSize: 18,
                        },
                      }}
                      label="Team Name"
                      sx={{
                        "& .MuiInput-underline:before": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                        "& .MuiInput-underline:hover:not(.Mui-disabled):before":
                          {
                            borderBottom: "1px solid #E0E0E0",
                          },
                        "& .MuiInput-underline:after": {
                          borderBottom: "1px solid #E0E0E0",
                        },
                      }}
                    />
                  </Grid>
                </Grid>
              </Grid>
              <Box
                mt={3}
                display="flex"
                width={"100%"}
                justifyContent="flex-end"
                gap={2}
              >
                <Button
                  onClick={onClose}
                  disabled={loader}
                  sx={{
                    bgcolor: "#F4F0FF",
                    color: "#0047AB",
                    border: "none",
                    fontSize: "14px",
                    fontWeight: "500",
                    textTransform: "none",
                    px: 4,
                    "&:hover": {
                      bgcolor: "#EDE9FE",
                    },
                  }}
                >
                  Cancel
                </Button>
                {loader ? (
                  <Button
                    disabled
                    sx={{
                      bgcolor: "#0047AB",
                      color: "#fff",
                      fontSize: "14px",
                      fontWeight: "500",
                      textTransform: "none",
                      px: 4,
                      "&:hover": {
                        bgcolor: "#1E40AF",
                      },
                    }}
                  >
                    Loading...
                  </Button>
                ) : (
                  <Button
                    type="submit"
                    sx={{
                      bgcolor: "#0047AB",
                      color: "#fff",
                      fontSize: "14px",
                      fontWeight: "500",
                      textTransform: "none",
                      px: 4,
                      "&:hover": {
                        bgcolor: "#1E40AF",
                      },
                    }}
                  >
                    {type === "Edit" ? "Update" : "Add"}
                  </Button>
                )}
              </Box>
            </Box>
          </DialogContent>
        </Box>
      </Dialog>

      <Dialog
        open={discard}
        onClose={() => setDiscard(false)}
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
        {/* Close Icon */}
        <IconButton
          onClick={() => setDiscard(false)}
          sx={{ position: "absolute", top: 12, right: 12 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>

        {/* Title */}
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "20px",
            p: 0,
            mb: 1,
          }}
        >
          {type === "team"
            ? "Delete Team?"
            : " Are You Sure You Want to Remove ?"}
        </DialogTitle>

        {/* Subtitle */}
        <DialogContent sx={{ px: 0 }}>
          <Typography
            sx={{
              color: "#95919D",
              fontSize: "16px",
              fontWeight: 400,
              width: "90%",
            }}
          >
            {type === "team"
              ? "Do you really want to delete this team?  This action cannot be undone."
              : " Once removed, Member will no longer have access to the team or its resources ."}
          </Typography>
        </DialogContent>

        {/* Buttons */}
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
            onClick={() => setDiscard(false)}
            disabled={deleteLoading}
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
            onClick={handleConfirm}
            disabled={deleteLoading}
            sx={{
              backgroundColor: "#D93A3A",
              color: "#fff",
              borderRadius: "8px",
              textTransform: "none",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#D93A3A",
              },
            }}
          >
            {deleteLoading ? "Deleting..." : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={openAddMember}
        onClose={() => setOpenAddMember(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{
          sx: { borderRadius: "16px", p: 1.5 },
        }}
      >
        {/* Title + Close */}
        <DialogTitle
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            fontWeight: "bold",
            fontSize: "20px",
          }}
        >
          Add Member
          <IconButton onClick={() => setOpenAddMember(false)} size="small">
            <CloseIcon />
          </IconButton>
        </DialogTitle>

        {/* Search Bar */}
        <DialogContent sx={{ px: 2 }}>
          <TextField
            placeholder="Search"
            fullWidth
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            variant="outlined"
            InputProps={{
              sx: {
                backgroundColor: "#F7F8FC",
                borderRadius: "12px",
                height: "42px",
                "& fieldset": { border: "none" },
              },
            }}
          />

          {/* Member List */}
          <List sx={{ mt: 2, maxHeight: 300, overflowY: "auto" }}>
            {filteredMembers?.map((member, idx) => (
              <ListItem key={idx}>
                <ListItemAvatar>
                  <Avatar src={member?.profilePictureUrl} />
                </ListItemAvatar>
                <ListItemText
                  primary={member.name}
                  secondary={member.email}
                  primaryTypographyProps={{ fontWeight: 600 }}
                  secondaryTypographyProps={{ color: "text.secondary" }}
                />
                <ListItemSecondaryAction>
                  <Checkbox
                    checked={selected.includes(member.id)}
                    onChange={() => handleToggle(member.id)}
                    sx={{
                      color: "#E5E7EB",
                      "&.Mui-checked": {
                        color: "#1E40AF", // dark blue when checked
                      },
                    }}
                  />
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </DialogContent>

        {/* Footer Buttons */}
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
            onClick={() => setOpenAddMember(false)}
            disabled={loader}
            sx={{
              backgroundColor: "#F5F3FF",
              color: "#4F46E5",
              px: 4,
              py: 1,
              borderRadius: "10px",
              textTransform: "none",
              fontWeight: 500,
              "&:hover": { backgroundColor: "#EDE9FE" },
            }}
          >
            Cancel
          </Button>
          {loader ? (
            <Button
              disabled
              sx={{
                px: 4,
                py: 1,
                backgroundColor: "#1E40AF",
                borderRadius: "10px",
                textTransform: "none",
                fontWeight: 500,
                color: "#fff",
                "&:hover": { backgroundColor: "#1D4ED8" },
              }}
            >
              Loading...
            </Button>
          ) : (
            <Button
              onClick={() => onConfirm(selected)}
              sx={{
                px: 4,
                py: 1,
                backgroundColor: "#1E40AF",
                borderRadius: "10px",
                textTransform: "none",
                fontWeight: 500,
                color: "#fff",
                "&:hover": { backgroundColor: "#1D4ED8" },
              }}
            >
              Add
            </Button>
          )}
        </DialogActions>
      </Dialog>
      {/* Version */}
      <Typography
        variant="caption"
        sx={{ mt: 2, textAlign: "center", display: "block", color: grey[500] }}
      >
        Version 24.01
      </Typography>
    </Card>
  );
};

export default AdminTeams;
