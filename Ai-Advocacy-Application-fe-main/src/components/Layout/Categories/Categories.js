import React, { useCallback, useEffect, useState } from "react";
import {
  Box,
  Card,
  IconButton,
  List,
  ListItem,
  ListItemText,
  TextField,
  Typography,
} from "@mui/material";
import DashboardRoundedIcon from "@mui/icons-material/DashboardRounded";
import FolderIcon from "@mui/icons-material/Folder";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import AddIcon from "@mui/icons-material/Add";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import ArrowDropUpIcon from "@mui/icons-material/ArrowDropUp";
import SettingsIcon from "@mui/icons-material/Settings";
import { green, red, blue } from "@mui/material/colors";
import { useLocation, useNavigate } from "react-router-dom";
import { Auth } from "../../../contexts/AuthContext";
import logoImg from "../../../assets/SocialRipplelogo_J.png";
import timeline from "../../../assets/Widget.png";
import timelineActive from "../../../assets/timeline.svg";
import UserService from "../../../services/categoryService";
import "./Categories.css";

const employeeMenuRoutes = [
  {
    path: "/employees/settings",
    name: "Settings",
    icon: <SettingsIcon sx={{ color: blue[700], width: 22, height: 22 }} />,
  },
];

const Categories = () => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const activeCategoryId = searchParams.get("category");
  const navigate = useNavigate();
  const { setCategoriesData, setCategoriesDetails } = Auth();
  const [categories, setCategories] = useState([]);
  const [newCategory, setNewCategory] = useState("");
  const [isAdding, setIsAdding] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const fetchAllCategories = useCallback(async () => {
    try {
      const response = await UserService.getAllCategories();
      if (response?.status === true) {
        setCategories(response?.categories);
        setCategoriesData(response?.categories);
      }
    } catch (error) {
      console.log(error);
    }
  }, [setCategoriesData]);

  useEffect(() => {
    fetchAllCategories();
  }, [fetchAllCategories]);

  const handleSaveCategory = () => {
    if (newCategory.trim() !== "") {
      setCategories([{ id: `new-${Date.now()}`, name: newCategory }, ...categories]);
      setNewCategory("");
      setIsAdding(false);
      setIsDropdownOpen(true);
    }
  };

  const handleDiscardCategory = () => {
    setNewCategory("");
    setIsAdding(false);
  };

  const handleNavigation = (category) => {
    setCategoriesDetails(category);
    navigate(`/employees?category=${category?.id}`);
  };

  const isActiveRoute = (path) => location.pathname === path;

  return (
    <Card className="category-container">
      <Box display="flex" alignItems="center" justifyContent="space-between">
        <Box>
          <img className="logo-img" src={logoImg} alt="Advocacy Logo" />
        </Box>
      </Box>

      <Box
        className={`ad_timeline ${location.pathname === "/employees" || location.pathname === "/employees/timeline" ? "active-route" : ""}`}
        onClick={() => navigate("/employees/timeline")}
      >
        <img
          src={location.pathname === "/employees/timeline" ? timelineActive : timeline}
          alt="timeline"
          style={{ width: 18, height: 18, marginRight: 11 }}
        />
        <Typography
          variant="body2"
          sx={{
            color: location.pathname === "/employees/timeline" ? "#fff" : "#000",
          }}
        >
          Timeline
        </Typography>
      </Box>

      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        className="setting-icon"
        sx={{
          cursor: "pointer",
          "&:hover": {
            background: "#e1e8f2",
            borderRadius: "8px",
          },
        }}
        onClick={() => setIsDropdownOpen(!isDropdownOpen)}
      >
        <Box display="flex" alignItems="center">
          <FolderIcon sx={{ color: "#f7c333", width: 22, height: 22 }} />
          <Typography variant="body2" fontWeight={400} marginLeft={2}>
            Categories
          </Typography>
        </Box>
        <IconButton size="small">
          {isDropdownOpen ? (
            <ArrowDropUpIcon sx={{ color: "#2D76DC" }} />
          ) : (
            <ArrowDropDownIcon sx={{ color: "#2D76DC" }} />
          )}
        </IconButton>
      </Box>

      <List>
        {isAdding && (
          <ListItem sx={{ display: "flex", alignItems: "center" }}>
            <Box
              sx={{
                width: 8,
                height: 8,
                backgroundColor: "#2D76DC",
                borderRadius: "50%",
                marginRight: 2,
              }}
            />
            <TextField
              value={newCategory}
              onChange={(event) => setNewCategory(event.target.value)}
              placeholder="Category Name"
              variant="standard"
              className="addCategory-field"
              InputProps={{ disableUnderline: true }}
            />
            <IconButton onClick={handleSaveCategory} sx={{ color: green[700] }}>
              <CheckIcon />
            </IconButton>
            <IconButton onClick={handleDiscardCategory} sx={{ color: red[700] }}>
              <CloseIcon />
            </IconButton>
          </ListItem>
        )}
        {isDropdownOpen && (
          <Box sx={{ overflow: "auto", maxHeight: "400px" }} className="scroll-container">
            {categories?.map((category, index) => {
              const isActive = String(activeCategoryId) === String(category.id);
              return (
                <ListItem
                  className="categoryList"
                  key={category?.id || index}
                  sx={{
                    display: "flex",
                    backgroundColor: isActive ? "#E3F2FD" : "transparent",
                    alignItems: "center",
                    borderRadius: 2,
                    cursor: "pointer",
                  }}
                  onClick={() => handleNavigation(category)}
                >
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      backgroundColor: isActive ? "#2D76DC" : "#B0BEC5",
                      borderRadius: "50%",
                      marginRight: 2,
                    }}
                  />
                  <ListItemText
                    sx={{
                      color: isActive ? "#2D76DC" : "#000000",
                      fontWeight: isActive ? 600 : 400,
                    }}
                    primaryTypographyProps={{ fontSize: "13px" }}
                    primary={category?.name}
                  />
                </ListItem>
              );
            })}
          </Box>
        )}

        <Box
          className="setting-icon"
          onClick={() => {
            setIsAdding((current) => !current);
            setIsDropdownOpen(true);
          }}
          sx={{ cursor: "pointer" }}
        >
          <Box display="flex" alignItems="center">
            <AddIcon sx={{ color: blue[700], width: 22, height: 22 }} />
            <Typography variant="body2" fontWeight={400} marginLeft={2}>
              Add New
            </Typography>
          </Box>
        </Box>

        {employeeMenuRoutes.map((route) => (
          <Box
            key={route.path}
            className={`setting-icon ${isActiveRoute(route.path) ? "active-route" : ""}`}
            onClick={() => navigate(route.path)}
            sx={{ cursor: "pointer" }}
          >
            <Box display="flex" alignItems="center">
              {route.icon}
              <Typography variant="body2" fontWeight={400} marginLeft={2}>
                {route.name}
              </Typography>
            </Box>
          </Box>
        ))}
      </List>
    </Card>
  );
};

export default Categories;
