import AddIcon from "@mui/icons-material/Add";
import ArrowDropDownIcon from "@mui/icons-material/ArrowDropDown";
import ArrowDropUpIcon from "@mui/icons-material/ArrowDropUp";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import FolderIcon from "@mui/icons-material/Folder";
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemText,
  TextField,
  Typography,
} from "@mui/material";
import { green, red } from "@mui/material/colors";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import photoicon from "../../../assets/GalleryWide.png";
import setting from "../../../assets/setting.svg";
import logoImg from "../../../assets/SocialRipplelogo_J.png";
import timeline2 from "../../../assets/timeline.svg";
import usericon from "../../../assets/UsersGroupTwoRounded.png";
import dashicon from "../../../assets/Vector.png";
import videoicon from "../../../assets/VideoLibrary.png";
import timeline from "../../../assets/Widget.png";
import { Auth } from "../../../contexts/AuthContext";
import UserService from "../../../services/categoryService";
import "../../Layout/Categories/Categories.css";

/**
 * AdminCategories Component
 *
 * A sidebar navigation component for the admin panel that displays and manages categories.
 * Provides functionality to view, add, edit, and delete categories with a collapsible list UI.
 *
 * @component
 * @returns {JSX.Element} The rendered AdminCategories sidebar component
 */
const AdminCategories = () => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const navigate = useNavigate();
  const activeCategoryId = searchParams.get("category");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [categories, setCategories] = useState([]);
  const [newCategory, setNewCategory] = useState("");
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editValue, setEditValue] = useState("");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState(null);
  const [saveLoading, setSaveLoading] = useState(false);
  const [deleteCatLoading, setDeleteCatLoading] = useState(false);
  const [editLoading, setEditLoading] = useState(false);
  const { setCategoriesData, setCategoriesDetails } = Auth();

  const [routes, setRoutes] = useState([
    {
      path: "/admin/user-management",
      name: "User Management",
      icon: usericon,
    },
    {
      path: "/admin/photo-library",
      name: "Photo Library",
      icon: photoicon,
    },
    {
      path: "/admin/video-library",
      name: "Video Library",
      icon: videoicon,
    },
    {
      path: "/admin/settings",
      name: "Settings",
      icon: setting,
    },
  ]);

  /**
   * Fetches all categories from the API and updates state
   * @async
   * @returns {Promise<void>}
   */
  const fetchAllCategories = async () => {
    try {
      const response = await UserService.getAllCategories();
      if (response?.status === true) {
        setCategories(response?.categories);
        setCategoriesData(response?.categories);
      }
    } catch (error) {
      // Error handled silently
    }
  };

  /**
   * Saves a new category to the API
   * Optimistically updates UI, then syncs with server
   * @async
   * @returns {Promise<void>}
   */
  const handleSaveCategory = async () => {
    if (newCategory.trim() === "") return;
    setSaveLoading(true);
    try {
      const payload = {
        name: newCategory,
      };
      const response = await UserService.addNewCategories(payload);
      if (response?.status === true) {
        setNewCategory("");
        setIsAdding(false);
        fetchAllCategories();
        toast.success("Category Added Successfully");
      } else {
        toast.error(response?.message || "Failed to add category");
      }
    } catch (error) {
      toast.error(error?.response?.data?.message || "Failed to add category");
    } finally {
      setSaveLoading(false);
    }
  };

  /**
   * Discards the new category input and hides the add form
   * @returns {void}
   */
  const handleDiscardCategory = () => {
    setNewCategory("");
    setIsAdding(false);
  };

  /**
   * Initiates inline editing mode for a category
   * @param {Object} category - The category object to edit
   * @param {number} category.id - Category ID
   * @param {string} category.name - Category name
   * @param {Event} e - Click event (stopped to prevent navigation)
   * @returns {void}
   */
  const handleStartEdit = (category, e) => {
    e.stopPropagation();
    setEditingId(category.id);
    setEditValue(category.name);
  };

  /**
   * Saves the edited category to the API
   * @async
   * @param {Event} e - Click/keyboard event (stopped to prevent navigation)
   * @returns {Promise<void>}
   */
  const handleSaveEdit = async (e) => {
    e.stopPropagation();
    if (editValue.trim() === "") return;
    setEditLoading(true);
    try {
      const payload = {
        name: editValue,
        code: editValue,
        description: editValue,
      };
      const response = await UserService.updateCategory(editingId, payload);
      if (response?.status === true) {
        fetchAllCategories();
        toast.success("Category Updated Successfully");
      }
    } catch (error) {
      toast.error("Failed to update category");
    } finally {
      setEditLoading(false);
      setEditingId(null);
      setEditValue("");
    }
  };

  /**
   * Cancels inline editing mode and resets edit state
   * @param {Event} e - Click event (stopped to prevent navigation)
   * @returns {void}
   */
  const handleCancelEdit = (e) => {
    e.stopPropagation();
    setEditingId(null);
    setEditValue("");
  };

  /**
   * Opens the delete confirmation dialog for a category
   * @param {Object} category - The category object to delete
   * @param {number} category.id - Category ID
   * @param {string} category.name - Category name (displayed in dialog)
   * @param {Event} e - Click event (stopped to prevent navigation)
   * @returns {void}
   */
  const handleOpenDeleteDialog = (category, e) => {
    e.stopPropagation();
    setCategoryToDelete(category);
    setDeleteDialogOpen(true);
  };

  /**
   * Closes the delete confirmation dialog and resets state
   * @returns {void}
   */
  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setCategoryToDelete(null);
  };

  /**
   * Confirms and executes category deletion via API
   * @async
   * @returns {Promise<void>}
   */
  const handleConfirmDelete = async () => {
    if (!categoryToDelete) return;
    setDeleteCatLoading(true);
    try {
      const response = await UserService.deleteCategory(categoryToDelete.id);
      if (response?.status === true) {
        fetchAllCategories();
        toast.success("Category Deleted Successfully");
      }
    } catch (error) {
      toast.error("Failed to delete category");
    } finally {
      setDeleteCatLoading(false);
      handleCloseDeleteDialog();
    }
  };

  useEffect(() => {
    fetchAllCategories();
  }, []);

  /**
   * Navigates to the timeline page for a selected category
   * @param {Object} category - The category object to navigate to
   * @param {number} category.id - Category ID used in URL query param
   * @returns {void}
   */
  const handleNavigation = (category) => {
    setCategoriesDetails(category);
    navigate(`/admin/timeline?category=${category?.id}`);
  };

  return (
    <Card className="category-container">
      {/* Header with Logo */}
      <Box display="flex" alignItems="center" justifyContent="space-between">
        <Box>
          <img className="logo-img" src={logoImg} alt="Advocacy Logo" />
        </Box>
      </Box>

      {/* Timeline Button */}
      <Box
        onClick={() => navigate("/admin")}
        className="ad_timeline"
        display={"flex"}
        alignItems={"center"}
        mt={2}
        mb={1.5}
        sx={{
          backgroundColor: location.pathname === "/admin" ? "#0047ab" : "",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            width: 22,
            height: 22,
            background: "#bed2e9",
            marginRight: 13,
            borderRadius: "8px",
          }}
        >
          <img src={dashicon} alt="timeline" width={13} height={13} />
        </div>
        <Typography
        variant="body2"
          sx={{
            color: location.pathname === "/admin" ? "#fff" : "#000",
            "&:hover": {
              color: "#fff !important",
            },
          }}
        >
          DashBoard
        </Typography>
      </Box>
      {/* Timeline Button */}
      <Box
        onClick={() => navigate("/admin/timeline")}
        pl={2.5}
        className="ad_timeline"
        display={"flex"}
        alignItems={"center"}
        mb={1.5}
        sx={{
          backgroundColor:
            location.pathname === "/admin/timeline" ? "#0047ab" : "",
        }}
      >
        <img
          src={location.pathname === "/admin/timeline" ? timeline2 : timeline}
          alt="timeline"
          style={{
            width: 19,
            height: 19,
            marginRight: 14,
          }}
        />
        <Typography
        variant="body2"
          sx={{
            color: location.pathname === "/admin/timeline" ? "#fff" : "#000",
            "&:hover": {
              color: "#fff !important",
            },
          }}
        >
          Timeline
        </Typography>
      </Box>

      {/* Categories Section */}
      <Box pl={1.5}>
        <Box
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          display="flex"
          justifyContent="space-between"
          alignItems="center"
        >
          <Box display="flex" alignItems="center">
            <FolderIcon className="folder" />
            <Typography className="header">Categories</Typography>
          </Box>

          <IconButton>
            {isDropdownOpen ? (
              <ArrowDropUpIcon sx={{ color: "#2D76DC" }} />
            ) : (
              <ArrowDropDownIcon sx={{ color: "#2D76DC" }} />
            )}
          </IconButton>
        </Box>

        <List>
          {isAdding && (
            <ListItem
              sx={{
                display: "flex",
                alignItems: "center",
              }}
            >
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
                onChange={(e) => setNewCategory(e.target.value)}
                placeholder="Category Name"
                variant="standard"
                className="addCategory-field"
                InputProps={{
                  disableUnderline: true,
                }}
              />
              <IconButton
                onClick={handleSaveCategory}
                disabled={saveLoading}
                sx={{ color: green[700] }}
              >
                <CheckIcon />
              </IconButton>
              <IconButton
                onClick={handleDiscardCategory}
                disabled={saveLoading}
                sx={{ color: red[700] }}
              >
                <CloseIcon />
              </IconButton>
            </ListItem>
          )}

          {/* Existing Categories */}
          {isDropdownOpen && (
            <Box
              sx={{
                maxHeight: 400,
                overflowY: "auto",
              }}
              className="scroll-container"
            >
              {categories?.length === 0 && !isAdding && (
                <ListItem
                  sx={{
                    color: "#64748B",
                    py: 1,
                  }}
                >
                  <ListItemText
                    primary="No categories yet. Add your first category."
                    primaryTypographyProps={{
                      fontSize: "13px",
                    }}
                  />
                </ListItem>
              )}
              {categories?.map((category, index) => {
                const isActive =
                  String(activeCategoryId) === String(category.id);
                const isEditing = editingId === category.id;

                return (
                  <ListItem
                    className="categoryList"
                    key={index}
                    sx={{
                      display: "flex",
                      backgroundColor: isActive ? "#E3F2FD" : "transparent",
                      alignItems: "center",
                      borderRadius: 2,
                      cursor: isEditing ? "default" : "pointer",
                      "&:hover .action-icons": {
                        opacity: 1,
                      },
                    }}
                    onClick={() => !isEditing && handleNavigation(category)}
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
                    {isEditing ? (
                      <>
                        <TextField
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          placeholder="Category Name"
                          variant="standard"
                          className="addCategory-field"
                          InputProps={{
                            disableUnderline: true,
                          }}
                          autoFocus
                          onClick={(e) => e.stopPropagation()}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") handleSaveEdit(e);
                            if (e.key === "Escape") handleCancelEdit(e);
                          }}
                        />
                        <IconButton
                          onClick={handleSaveEdit}
                          disabled={editLoading}
                          sx={{ color: green[700] }}
                          size="small"
                        >
                          <CheckIcon fontSize="small" />
                        </IconButton>
                        <IconButton
                          onClick={handleCancelEdit}
                          disabled={editLoading}
                          sx={{ color: red[700] }}
                          size="small"
                        >
                          <CloseIcon fontSize="small" />
                        </IconButton>
                      </>
                    ) : (
                      <>
                        <ListItemText
                          sx={{
                            color: isActive ? "#2D76DC" : "#000000",
                            fontWeight: isActive ? 600 : 400,
                            flex: 1,
                          }}
                          primary={category?.name}
                          primaryTypographyProps={{
                            fontSize: "13px",
                          }}
                        />
                        <Box
                          className="action-icons"
                          sx={{
                            opacity: 0,
                            transition: "opacity 0.2s",
                            display: "flex",
                          }}
                        >
                          <IconButton
                            onClick={(e) => handleStartEdit(category, e)}
                            size="small"
                            sx={{ color: "#2D76DC" }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            onClick={(e) => handleOpenDeleteDialog(category, e)}
                            size="small"
                            sx={{ color: red[500] }}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Box>
                      </>
                    )}
                  </ListItem>
                );
              })}
            </Box>
          )}

          <Box
            display="flex"
            onClick={() => {
              setIsAdding(!isAdding);
              setIsDropdownOpen(!isDropdownOpen);
            }}
            bgcolor={"#F8F6FD"}
            alignItems="center"
          >
            <IconButton>
              <AddIcon sx={{ color: "#2D76DC" }} />
            </IconButton>
            <Typography fontWeight={400} fontSize={14} marginLeft={2}>
              Add New
            </Typography>
          </Box>
        </List>
      </Box>

      {/* Categories Section */}
      {routes?.map((route, index) => {
        const isActive = location.pathname === route.path;
        return (
          <Box
            key={index}
            className={`setting-icon ${isActive ? "active-route" : ""}`}
            onClick={() => navigate(route.path)}
          >
            <Box display="flex" alignItems="center">
              <img
                style={{ width: 22, height: 22 }}
                src={route.icon}
                alt={route.name}
              />
              <Typography variant="body2" fontWeight={400} marginLeft={2}>
                {route.name}
              </Typography>
            </Box>
          </Box>
        );
      })}

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleCloseDeleteDialog}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle id="delete-dialog-title">Delete Category</DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            Are you sure you want to delete "{categoryToDelete?.name}"?
            <br />
            <br />
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog} color="inherit" disabled={deleteCatLoading}>
            Cancel
          </Button>
          <Button onClick={handleConfirmDelete} color="error" variant="contained" disabled={deleteCatLoading}>
            {deleteCatLoading ? "Deleting..." : "Delete"}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
};

export default AdminCategories;
