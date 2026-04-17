import React, { useEffect, useState } from "react";
import {
  Card,
  Typography,
  Box,
  List,
  ListItem,
  ListItemText,
  Collapse,
  Avatar,
} from "@mui/material";
import { grey } from "@mui/material/colors";
import "./Teams.css";
import Position1 from "../../../assets/Position-1.svg";
import Position2 from "../../../assets/Position-2.svg";
import Position3 from "../../../assets/Position-3.svg";
import coins from "../../../assets/coins.svg";
import { useNavigate, useParams } from "react-router-dom";
import {
  getAllLeaders,
  getAllTeams,
  getTeamDetails,
} from "../../../services/teamServices";

const svgIcons = [Position1, Position2, Position3];

const Teams = () => {
  const { id } = useParams();
  const [activeTeam, setActiveTeam] = useState(id);
  const [openTeamId, setOpenTeamId] = useState(null);
  const [TeamDetails, setTeamDetails] = useState([]);
  const [activeTab, setActiveTab] = useState("Leaderboard");
  const [Teams, setTeams] = useState([]);
  const [leaderBoard, setLeaderBoard] = useState([]);
  const navigate = useNavigate();

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
      console.log(response, "respomse");
      if (response?.status === true) {
        setLeaderBoard(response?.leaderList);
      }
    } catch (error) {
      console.error("Error fetching teams:", error);
    }
  };

  useEffect(() => {
    fetchAllLeaders();
    fetchAllTeams();
  }, []);

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

  const handleMemberClick = (member) => {
    navigate(`/employees/team-member/${member?.userId}`);
    setActiveTeam(member?.userId);
  };

  return (
    <Card className="teams-container">
      {/* Header */}
      <p className="teams-header">Teams</p>

      {/* Tabs */}
      <Box
        display="flex"
        justifyContent="space-between"
        sx={{
          mb: 2,
          mt: 2,
        }}
      >
        <Box
          onClick={() => setActiveTab("Leaderboard")}
          sx={{
            flex: 1,
            textAlign: "center",
            cursor: "pointer",
            padding: "8px 0",
            fontWeight: activeTab === "Leaderboard" ? 500 : 400,
            color: activeTab === "Leaderboard" ? "#3B3B3B" : "#95919D",
            borderBottom:
              activeTab === "Leaderboard" ? `2px solid #2D76DC` : "none",
          }}
          fontSize={14}
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
            fontWeight: activeTab === "All Teams" ? 500 : 400,
            color: activeTab === "All Teams" ? "#3B3B3B" : "#95919D",
            borderBottom:
              activeTab === "All Teams" ? `2px solid #2D76DC` : "none",
          }}
          fontSize={14}
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
              sx={{
                display: "flex",
                alignItems: "center",
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
                primary={team?.userName}
              />

              {/* Player Points */}
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  color: "#8296B2",
                  fontWeight: 400,
                  fontFamily: "Poppins",
                  fontSize: "14px",
                }}
              >
                <img
                  src={coins}
                  alt="coins"
                  style={{
                    width: 18,
                    height: 18,
                    marginRight: 3,
                  }}
                />
                {team?.totalPoints}
              </Box>
            </ListItem>
          ))}
        </List>
      )}

      {/* All Teams Tab */}
      {activeTab === "All Teams" && (
        <List
          sx={{ height: "43vh", overflowY: "auto" }}
          className="scroll-container"
        >
          {Teams?.map((team, index) => (
            <React.Fragment key={index}>
              <ListItem
                onClick={() => toggleSection(team)}
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "8px 16px",
                  cursor: "pointer",
                }}
              >
                <Box display="flex" alignItems="center">
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
                <Box>
                  {openTeamId === team?.teamId ? (
                    <svg
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
                      width="11"
                      height="6"
                      viewBox="0 0 11 6"
                      fill="none"
                      xmlns="http://www.w3.org/2000/svg"
                    >
                      <path d="M5.5 5.5L11 0.5H0L5.5 5.5Z" fill="#2D76DC" />
                    </svg>
                  )}
                </Box>
              </ListItem>
              <Collapse
                // in={expandedSections[index]}
                in={openTeamId === team?.teamId}
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
                          },
                          backgroundColor:
                            Number(activeTeam) === Number(member.userId)
                              ? "#E1E8F2"
                              : "transparent",
                          cursor: "pointer",
                        }}
                        onClick={() => handleMemberClick(member)}
                      >
                        <Box
                          sx={{
                            width: 8,
                            height: 8,
                            borderRadius: "50%",
                            marginRight: 2,
                            backgroundColor:
                              Number(activeTeam) === Number(member.userId)
                                ? "#2D76DC"
                                : "#D9D9D9",
                          }}
                        />
                        <ListItemText
                          primaryTypographyProps={{
                            fontSize: "13px", // directly applies to inner Typography
                          }}
                          primary={member?.name}
                        />
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
      )}
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

export default Teams;
