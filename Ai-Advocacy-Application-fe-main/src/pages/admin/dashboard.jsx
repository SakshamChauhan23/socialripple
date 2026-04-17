import {
  Box,
  Card,
  Typography,
  Grid,
  Stack,
} from "@mui/material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  ResponsiveContainer,
  Tooltip,
  AreaChart,
  Area,
  CartesianGrid,
} from "recharts";
import InstagramIcon from "../../assets/insta.png";
import FacebookIcon from "../../assets/facebook.png";
import LinkedInIcon from "../../assets/linkedin.png";
import TwitterIcon from "../../assets/twitter.png";
import {
  getAllEngagement,
  getAllMonthlyParticipation,
  getAllTopContributors,
  getLeadConversation,
  getMonthlyReach,
} from "../../services/adminServices";
import { useEffect, useState } from "react";
import { Auth } from "../../contexts/AuthContext";

const EmptyChartState = ({ message }) => (
  <Box
    sx={{
      height: "100%",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      px: 3,
      textAlign: "center",
    }}
  >
    <Typography sx={{ color: "#95919D", fontSize: 14 }}>
      {message}
    </Typography>
  </Box>
);

const AdminDashboard = () => {
  const {userProfile} = Auth()
  console.log(userProfile,"userProfile")
  const [engagement, setEngagement] = useState(null);
  const [monthlyParticipation, setMonthlyParticipation] = useState([]);
  const [monthlyReach, setMonthlyReach] = useState([]);
  const [contributors, setContributors] = useState(null);
  const [leadConversation, setLeadConversation] = useState(null);
  const hasMonthlyParticipation = monthlyParticipation.length > 0;
  const hasMonthlyReach = monthlyReach.length > 0;
  const statCards = [
    {
      label: "Employee Adoption",
      value: `${engagement?.employeeAdoptionPercentage || "0"}%`,
      subtext: "Of Employees Have Joined",
      color: "#1CA525",
    },
    {
      label: "Active Participation",
      value: `${engagement?.monthlyParticipation || "0"}`,
      subtext: "Employees Shared Content This Month",
      color: "#A7AE19",
    },
    {
      label: "Total Shares",
      value: `${engagement?.totalEngagements || "0"}`,
      subtext: "Employee shares recorded this year",
      color: "#4720B4",
    },
    {
      label: "Earned Media Value",
      value: `$${engagement?.estimatedMediaValueForCurrentYear || "0"}`,
      subtext: "Estimated Value From Advocacy Efforts",
      color: "#FDAF00",
    },
  ];
  const fetchAllEngagement = async () => {
    try {
      const response = await getAllEngagement();
      if (response?.status === true) {
        setEngagement(response?.employeeEngagement);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchAllMonthly = async () => {
    try {
      const response = await getAllMonthlyParticipation();
      if (response?.status === true) {
        setMonthlyParticipation(response?.monthlyParticipation);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchAllContributors = async () => {
    try {
      const response = await getAllTopContributors();
      if (response?.status === true) {
        setContributors(response?.topContributors);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchMonthlyReach = async () => {
    try {
      const response = await getMonthlyReach();
      if (response?.status === true) {
        setMonthlyReach(response?.monthlyReachCount);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const fetchLeadConversation = async () => {
    try {
      const response = await getLeadConversation();
      console.log(response,"leadConversation")
      if (response?.status === true) {
        setLeadConversation(response);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  useEffect(() => {
    fetchAllEngagement();
    fetchAllMonthly();
    fetchAllContributors();
    fetchMonthlyReach();
    fetchLeadConversation();
  }, []);
  return (
    <Box sx={{ margin: "auto", mb: 5 }}>
      <Typography
        variant="h5"
        fontSize={"20px"}
        color="#0047AB"
        sx={{ fontWeight: 500, mt: 2, mb: 1.5, ml: 2 }}
      >
        Hello {userProfile?.name || "User"},
      </Typography>
      <Typography
        variant="h6"
        color="#95919D"
        fontSize={"16px"}
        sx={{ fontWeight: 400, mb: 3, ml: 2 }}
      >
        Welcome To Your Advocacy Dashboard!
      </Typography>

      {/* Stats Cards */}
      <Grid container spacing={1} mb={1.4}>
        {statCards.map((card, i) => (
          <Grid item xs={12} sm={6} md={3} key={i}>
            <Card
              elevation={0}
              sx={{
                p: 2,
                height: "94px",
                borderRadius: "16px",
                bgcolor: "#FFFFFF",
              }}
            >
              <Typography
                variant="subtitle1"
                fontSize={"13px"}
                fontWeight={500}
                color="#3B3B3B"
              >
                {card.label}
              </Typography>
              <Typography
                variant="h5"
                fontSize={"16px"}
                sx={{
                  fontWeight: 500,
                  color: card.color || "#333",
                  mb: 0.5,
                  mt: 0.5,
                }}
              >
                {card.value}
              </Typography>

              <Typography fontSize={"11px"} variant="body2" color="#95919D">
                {card.subtext}
              </Typography>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={1.4}>
        {/* Bar Chart */}
        <Grid item xs={12} md={9}>
          <Card
            elevation={0}
            sx={{
              p: 2,
              borderRadius: "20px",
            }}
          >
            <Typography
              variant="h6"
              fontSize={"15px"}
              sx={{
                fontWeight: 500,
                color: "#000000",
                mb: 1.4,
              }}
            >
              Employee Participation Over Time
            </Typography>
            <Box height={280}>
              {hasMonthlyParticipation ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlyParticipation}>
                    <CartesianGrid
                      strokeDasharray="6 4"
                      vertical={false}
                      strokeWidth={0.5}
                      stroke="#95919D"
                    />
                    <XAxis
                      dataKey="name"
                      tick={{ fill: "#95919D", fontSize: 10, fontWeight: 500 }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <YAxis
                      tick={{ fill: "#9CA3AF", fontSize: 12 }}
                      axisLine={false}
                      tickLine={false}
                      domain={[
                        0,
                        (dataMax) => Math.ceil(dataMax + dataMax * 0.1)
                      ]}
                      label={{
                        value: "Number Of Active Employees",
                        angle: -90,
                        position: "outsideLeft",
                        offset: 20,
                        dx: -10,
                        style: { fill: "#CFCACA", fontSize: 12 },
                      }}
                    />
                    <Tooltip
                      cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                      contentStyle={{
                        borderRadius: 8,
                        fontSize: 12,
                      }}
                    />
                    <Bar
                      dataKey="value"
                      fill="#CB139D"
                      radius={[10, 10, 0, 0]}
                      barSize={9.4}
                    />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyChartState message="No employee participation data is available for this organization yet." />
              )}
            </Box>
          </Card>
        </Grid>

        {/* Contributors */}
        <Grid item xs={12} md={3}>
          <Card
            elevation={0}
            sx={{ p: 2, borderRadius: "16px", bgcolor: "#FFFFFF" }}
          >
            <Typography
              variant="body1"
              fontSize={"15px"}
              color="#000000"
              fontWeight={500}
              mb={2}
            >
              Top Contributors
            </Typography>

            <Stack spacing={1}>
              {contributors?.slice(0, 8)?.map((user, index) => (
                <Box
                  key={index}
                  display="flex"
                  justifyContent="space-between"
                  alignItems="center"
                >
                  <Box mb={1.23} display="flex" alignItems="center" gap={1}>
                    <Typography
                      variant="body1"
                      color="#606060"
                      display={"flex"}
                      gap={2}
                      fontSize={"12px"}
                    >
                      {index + 1}{" "}
                      <Typography
                        fontSize={"12px"}
                        color="#606060"
                        variant="body1"
                      >
                        {user?.userName || ""}
                      </Typography>
                    </Typography>
                  </Box>
                  <Typography
                    display={"flex"}
                    variant="body2"
                    fontWeight={300}
                    sx={{ color: "#95919D" }}
                    gap={1}
                    fontSize={"12px"}
                  >
                    Shares:{" "}
                    <Typography
                      color="#2D76DC"
                      fontSize={"12px"}
                      variant="body1"
                    >
                      {user?.totalShares || ""}
                    </Typography>
                  </Typography>
                </Box>
              ))}
            </Stack>
          </Card>
        </Grid>

        {/* Area Chart */}
        <Grid item xs={12} md={9}>
          <Card
            elevation={0}
            sx={{
              bgcolor: "#fff",
              borderRadius: "20px",
              px: 2,
              pt: 2,
              pb: 0.5,
            }}
          >
            <Typography
              sx={{
                fontSize: "15px",
                fontWeight: 500,
                color: "#000000",
                mb: 2,
              }}
            >
              Reach & Amplification
            </Typography>

            <Box height={320}>
              {hasMonthlyReach ? (
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart
                    data={monthlyReach}
                    margin={{ top: 10, right: 20, left: 10, bottom: 0 }}
                  >
                    <defs>
                      <linearGradient
                        id="blueGradient"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop offset="0%" stopColor="#6059E3" stopOpacity={1} />
                        <stop
                          offset="100%"
                          stopColor="#6059E3"
                          stopOpacity={0.02}
                        />
                      </linearGradient>
                    </defs>

                    <CartesianGrid
                      strokeDasharray="3 3"
                      stroke="#E5E7EB"
                      vertical={false}
                    />
                    <XAxis
                      dataKey="name"
                      tick={{ fill: "#9CA3AF", fontSize: 10 }}
                      axisLine={false}
                      tickLine={false}
                    />
                    <YAxis
                      tick={{ fill: "#9CA3AF", fontSize: 10 }}
                      axisLine={false}
                      tickLine={false}
                      domain={[
                        0,
                        (dataMax) => Math.ceil(dataMax + dataMax * 0.1)
                      ]}
                      ticks={(() => {
                        const maxVal = Math.max(...monthlyReach.map((d) => d.value));
                        const step = Math.max(1, Math.ceil(maxVal / 5));
                        return Array.from({ length: 6 }, (_, i) => i * step);
                      })()}
                      width={60}
                      label={{
                        value: "Total Reach (Impressions & Shares)",
                        angle: -90,
                        position: "insideLeft",
                        style: {
                          textAnchor: "middle",
                          fill: "#9CA3AF",
                          fontSize: 10,
                        },
                      }}
                    />
                    <Tooltip
                      cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                      contentStyle={{
                        borderRadius: 8,
                        fontSize: 12,
                      }}
                    />
                    <Area
                      type="monotone"
                      dataKey="value"
                      stroke="#2563EB"
                      strokeWidth={2.5}
                      fill="url(#blueGradient)"
                      dot={false}
                      activeDot={false}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <EmptyChartState message="No reach or amplification data is available for this organization yet." />
              )}
            </Box>
          </Card>
        </Grid>

        {/* Leads & Conversion */}
        <Grid item xs={12} md={3}>
          <Card
            elevation={0}
            sx={{
              borderRadius: "16px",
              px: 2,
              py: 2,
            }}
          >
            <Typography
              sx={{
                fontSize: "15px",
                fontWeight: 500,
                color: "#111827",
                mb: 2,
              }}
            >
              Impressions & Click Through Rate
            </Typography>

            <Stack spacing={2} mb={2.4}>
              {/* Instagram */}
              <Box display="flex" alignItems="center" gap={1.5}>
                <img
                  src={InstagramIcon}
                  alt="Instagram"
                  width={30}
                  height={30}
                  style={{ borderRadius: 8 }}
                />
                <Box sx={{ minWidth: 70 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Impressions:
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#2D76DC", fontWeight: 500 }}>
                    {leadConversation?.instagramLead?.leadCount || 0}
                  </Typography>
                </Box>
                <Box sx={{ minWidth: 90 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                   Click Through Rate
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#1CA525", fontWeight: 500 }}>
                    {Number(leadConversation?.instagramLead?.conversionPercentage || 0).toFixed(2)}%
                  </Typography>
                </Box>
              </Box>

              {/* Facebook */}
              <Box display="flex" alignItems="center" gap={1.5}>
                <img
                  src={FacebookIcon}
                  alt="Facebook"
                  width={30}
                  height={30}
                  style={{ borderRadius: 8 }}
                />
                <Box sx={{ minWidth: 70 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Impressions:
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#2D76DC", fontWeight: 500 }}>
                    {leadConversation?.facebookLead?.leadCount || 0}
                  </Typography>
                </Box>
                <Box sx={{ minWidth: 90 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Click Through Rate
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#1CA525", fontWeight: 500 }}>
                    {Number(leadConversation?.facebookLead?.conversionPercentage || 0).toFixed(2)}%
                  </Typography>
                </Box>
              </Box>

              {/* LinkedIn */}
              <Box display="flex" alignItems="center" gap={1.5}>
                <img
                  src={LinkedInIcon}
                  alt="LinkedIn"
                  width={30}
                  height={30}
                  style={{ borderRadius: 8 }}
                />
                <Box sx={{ minWidth: 70 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Impressions:
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#2D76DC", fontWeight: 500 }}>
                    {leadConversation?.linkedinLead?.leadCount || 0}
                  </Typography>
                </Box>
                <Box sx={{ minWidth: 90 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                   Click Through Rate
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#1CA525", fontWeight: 500 }}>
                    {Number(leadConversation?.linkedinLead?.conversionPercentage || 0).toFixed(2)}%
                  </Typography>
                </Box>
              </Box>

              {/* Twitter/X */}
              <Box display="flex" alignItems="center" gap={1.5}>
                <img
                  src={TwitterIcon}
                  alt="Twitter"
                  width={30}
                  height={30}
                  style={{ borderRadius: 8 }}
                />
                <Box sx={{ minWidth: 70 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Impressions:
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#2D76DC", fontWeight: 500 }}>
                    {leadConversation?.xLead?.leadCount > 0 ? leadConversation?.xLead?.leadCount : "Unavailable"}
                  </Typography>
                </Box>
                <Box sx={{ minWidth: 90 }}>
                  <Typography sx={{ fontSize: 10, color: "#95919D" }}>
                    Click Through Rate
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: "#95919D", fontWeight: 500 }}>
                    {leadConversation?.xLead?.leadCount > 0
                      ? `${Number(leadConversation?.xLead?.conversionPercentage || 0).toFixed(2)}%`
                      : "Unavailable"}
                  </Typography>
                </Box>
              </Box>
            </Stack>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default AdminDashboard;
