import React, { useState, useEffect } from "react";
import { Card, CardContent, Avatar, Typography, Box, Divider } from "@mui/material";
import './Userdetail.css'
import Posts from '../../../assets/Posts.svg'
import Coins from '../../../assets/Profile-coins.svg'


const UserDetails = () => {
    const [LoggedInUser, setLoggedInUser] = useState(null);

    useEffect(() => {
        const storedUser = localStorage.getItem("userProfile");
        if (storedUser) {
            setLoggedInUser(JSON.parse(storedUser));
        }
    }, []);
    return (
        <Card className="userDetail-container">
            {/* Background Image */}
            <Box
                className="back-img"
            ></Box>

            {/* Content */}
            <CardContent sx={{ px: 2 }}>
                {
                    LoggedInUser?.profilePhotoUrl ? (
                        <Avatar src={LoggedInUser.profilePhotoUrl} alt={LoggedInUser.firstName} sx={{
                            width: 85,
                            height: 85,
                            border: "4px solid #2D76DC",
                            margin: "14px auto",
                        }} />
                    ) : (
                        <Avatar sx={{
                            width: 85,
                            height: 85,
                            border: "4px solid #2D76DC",
                            margin: "14px auto",
                        }}>{LoggedInUser?.firstName?.[0]}</Avatar>
                    )
                }
                <Typography variant="h6" className="userDetails">
                    {LoggedInUser?.firstName + " " + LoggedInUser?.lastName}
                </Typography>
                <Typography
                    variant="body2"
                    color="#95919D"

                    sx={{ mt: 0.5, fontSize: "13px" }}
                >
                    Digital Marketer | Marketing Team
                </Typography>

                {/* Stats */}
                <Box
                    sx={{
                        display: "flex",
                        justifyContent: "space-around",
                        alignItems: "center",
                        mt: 2,
                        backgroundColor: "#FFFFFF",
                        borderRadius: 2,
                        padding: 1,
                    }}
                >
                    {/* Coins */}
                    <Box sx={{ display: "flex", alignItems: "center" }}>
                        <img
                            src={Coins} // Replace with your coin logo path
                            alt="Coins"
                            style={{ width: 24, height: 24, marginRight: 8 }}
                        />
                        <Typography variant="subtitle1">
                            4633
                        </Typography>
                    </Box>
                    <Divider
                        orientation="vertical"
                        flexItem
                        sx={{
                            mx: 2,
                            height: "40px",
                            backgroundColor: "#EFF2F5",
                        }}
                    />
                    {/* Posts */}
                    <Box sx={{ display: "flex", alignItems: "center" }}>
                        <img
                            src={Posts} // Replace with your post logo path
                            alt="Posts"
                            style={{ width: 24, height: 24, marginRight: 8 }}
                        />
                        <Typography variant="subtitle1">
                            46
                        </Typography>
                    </Box>
                </Box>
            </CardContent>
        </Card>
    );
};

export default UserDetails;
