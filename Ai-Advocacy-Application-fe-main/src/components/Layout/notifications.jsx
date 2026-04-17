import { Avatar, Box, Card, Typography } from "@mui/material";
import { getAllNotifications } from "../../services/adminServices";
import { useEffect, useState } from "react";
import { formatNotification } from "../../shared/notificationFormatter";
const Notifications = () => {
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(10);
  const [notificationsList, setNotificationsList] = useState([]);


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
  useEffect(() => {
    fetchAllNotifications();
  }, []);
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
  return (
    <Card
      className="settings-container"
      elevation={0}
      sx={{
        borderRadius: "0px !important",
        background: "none !important",
        p: 0,
      }}
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
            backgroundColor: "#FFF",
            border: "1px solid #F2F2F2",
            borderRadius: 1,
            p: 2,
            mb: 1.5,
            zIndex: 1000,
          }}
        >
          <Avatar
            sx={{
              width: 30,
              height: 30,
            }}
            // src={item.icon}
          />

          <Box flex={1}>
            <Typography variant="subtitle1" color="#3B3B3B" fontWeight={600}>
              {formattedNotification.title}
            </Typography>
            <Typography variant="body2" color="#999999">
              {formattedNotification.message}
            </Typography>
          </Box>
          <Typography variant="caption" color="#B5B5B5">
          {item?.createdAt ? getTimeAgo(item?.createdAt) : ""}
          </Typography>
        </Box>
      )})}
    </Card>
  );
};

export default Notifications;
