import {
  Box,
  Card,
  Typography,
  MenuItem,
  Select,
  FormControl,
  Button,
  IconButton,
  Collapse,
  Dialog,
  DialogTitle,
  DialogContent,
  LinearProgress,
  TextField,
  CircularProgress,
  DialogActions,
  useTheme,
  useMediaQuery,
  Autocomplete,
  Popper,
  Chip,
} from "@mui/material";
import archives from "../../assets/achives.png";
import bulk from "../../assets/bulkedit.png";
import img1 from "../../assets/profile/img2.png";
import { useEffect, useState } from "react";
import CloseIcon from "@mui/icons-material/Close";
import UploadIcon from "../../assets/GalleryAdd.png";
import tagIcon from "../../assets/Tag.png";
import deleteicon from "../../assets/trash.png";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";
import { Auth } from "../../contexts/AuthContext";
import {
  AddMediaToLibrary,
  changeMediaToArchive,
  deleteMediaById,
  getAllAdminMedia,
  restoreMedia,
  uploadMedia,
  uploadZipFile,
} from "../../services/postService";
import { toast } from "react-toastify";
import { employeeTrendingListing } from "../../services/adminServices";

const ImageManagement = () => {
  const { categoriesData } = Auth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const [expandedIndex, setExpandedIndex] = useState(null);
  const [image, setImage] = useState(null);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [imageIds, setImageIds] = useState([]);
  const [tags, setTags] = useState([]);
  const [tagInput, setTagInput] = useState("");
  const [open1, setOpen1] = useState(false);
  const [open, setOpen] = useState(false);
  const [bulkUpload, setBulkUpload] = useState(false);
  const [totalItems, setTotalItems] = useState(0);
  const [successCount, setSuccessCount] = useState(0);
  const [failedCount, setFailedCount] = useState(0);
  const [totalMedia, setTotalMedia] = useState([]);
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(0);
  const [page, setPage] = useState(0);
  const [limit, setLimit] = useState(20);
  const [loader, setLoader] = useState(false);
  const [selectedCategoryId, setSelectedCategoryId] = useState();
  const [singleData, setSingleData] = useState({});
  const [archived, setArchived] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [restoreLoading, setRestoreLoading] = useState(false);
  const [archiveLoading, setArchiveLoading] = useState(false);
  const [trendsData, setTrendsData] = useState([]);
  const [trendsInput, setTrendsInput] = useState("");
  const [selectedTrends, setSelectedTrends] = useState([]);

  const addTrends = (newValue) => {
    if (!newValue) {
      setTrendsInput("");
      return;
    }
    const tags = String(newValue)
      .split(",")
      .map((t) => t.trim())
      .filter(Boolean);
    if (tags.length === 0) {
      setTrendsInput("");
      return;
    }
    setSelectedTrends((prev) => {
      const next = [...prev];
      for (const tag of tags) {
        if (!next.includes(tag)) next.push(tag);
      }
      return next;
    });
    setTrendsInput("");
  };
  const removeTrends = (trend) => {
    setSelectedTrends((prev) => prev.filter((t) => t !== trend));
  };
  const fetchAllTrendingTopics = async () => {
    try {
      const response = await employeeTrendingListing();
      if (response?.status === true) {
        setTrendsData(response?.trendingHashtags || []);
      }
    } catch (error) {
      console.log(error, "error");
    }
  };
  const handleCardClick = (index) => {
    setExpandedIndex(expandedIndex === index ? null : index);
  };
  const handleFileUpload = async (file) => {
    const reader = new FileReader();
    const formData = new FormData();

    console.log(file, "file");

    formData.append("files", file);
    formData.append("mediaType", "image");
    setLoader(true);
    try {
      const response = await uploadMedia(formData);
      if (response?.status === true) {
        setImageIds(response?.mediaUploadData?.imageIds);
        reader.onload = (e) => setImage(e.target.result);
        reader.readAsDataURL(file);
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setLoader(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) handleFileUpload(file);
  };

  const handleBrowse = (e) => {
    const file = e.target.files[0];
    if (file) handleFileUpload(file);
  };

  const onClose = () => {
    setBulkUpload(false);
    setImage(null);
    setOpen(false);
    setTitle("");
    setCategory("");
    setTags([]);
    setTagInput("");
  };

  const handleOpen = () => {
    setTrendsInput("");
    setSelectedTrends([]);
    setOpen(true);
  };

  const handleZipFileUpload = async (e) => {
    const uploadedFile = e.target.files[0];

    try {
      if (uploadedFile) {
        setFile(uploadedFile);
        const payload = new FormData();
        payload.append("zipFile", uploadedFile);
        payload.append("mediaType", "IMAGE");
        const response = await uploadZipFile(payload);
        if (response?.status) {
          toast.success("Bulk upload successful!");
          const { successCount, imageIds, failedCount, totalFiles } =
            response.mediaUploadData;
          setImageIds(imageIds);
          setSuccessCount(successCount);
          setFailedCount(failedCount);
          setTotalItems(totalFiles);
          setProgress(100);
        } else {
          setProgress(0);
          setSuccessCount(0);
          setFailedCount(0);
        }
      }
    } catch (error) {
      console.error("Upload error:", error);
      setProgress(0);
      setSuccessCount(0);
      setFailedCount(0);
    } finally {
    }
  };

  const handleDeleteFile = () => {
    setFile(null);
    setProgress(0);
    setTotalItems(0);
    setSuccessCount(0);
    setFailedCount(0);
  };
  const handleBulkUpload = () => {
    setTrendsInput("");
    setSelectedTrends([]);
    setBulkUpload(true);
  };

  const handleConfirm = async () => {
    try {
      if (!title?.trim()) {
        toast.error("Please enter a title.");
        return;
      }
      if (!category) {
        toast.error("Please select a category.");
        return;
      }
      if (!imageIds || imageIds.length === 0) {
        toast.error("Please upload image.");
        return;
      }
      if (!selectedTrends || selectedTrends.length === 0) {
        toast.error("Please add at least one hashtag.");
        return;
      }

      const payload = {
        mediaIds: imageIds,
        title: title,
        mediaType: "image",
        categoryId: Number(category),
        hashtags: selectedTrends,
      };
      setLoader(true);
      const response = await AddMediaToLibrary(payload);
      if (response?.status === true) {
        toast.success("Image added to library successfully!");
        onClose();
        setImageIds([]);
        setTitle("");
        setCategory("");
        setTags([]);
        setTagInput("");
        fetchAllMedia("all");
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoader(false);
    }
  };

  const handleZipConfirm = async () => {
    try {
      if (!category) {
        toast.error("Please select a category.");
        return;
      }
      if (!imageIds || imageIds.length === 0) {
        toast.error("Please upload image.");
        return;
      }
      if (!selectedTrends || selectedTrends.length === 0) {
        toast.error("Please add at least one hashtag.");
        return;
      }

      const payload = {
        mediaIds: imageIds,
        title: title,
        mediaType: "image",
        categoryId: Number(category),
        hashtags: selectedTrends,
      };
      setLoader(true);
      const response = await AddMediaToLibrary(payload);
      if (response?.status === true) {
        toast.success("Image added to library successfully!");
        onClose();
        setImageIds([]);
        setTitle("");
        setCategory("");
        setTags([]);
        setTagInput("");
        fetchAllMedia("all");
      }
    } catch (error) {
      console.log(error);
    } finally {
      setLoader(false);
    }
  };

  // const fetchAllMedia = async () => {
  //   try {
  //     const payload = { type: "image", categoryId: selectedCategoryId };
  //     const response = await getAllMediaByType(payload, page, limit);
  //     if (response?.status === true) {
  //       setTotalMedia(response?.mediaLibrary);
  //     }
  //   } catch (error) {
  //     console.log(error);
  //   }
  // };
  const fetchAllMedia = async (data) => {
    try {
      const payload = {
        type: "image",
        categoryId: selectedCategoryId,
        status: data,
      };
      const response = await getAllAdminMedia(payload, page, limit);
      if (response?.status === true) {
        setTotalMedia(response?.mediaLibrary);
      }
    } catch (error) {
      console.log(error);
    }
  };

  useEffect(() => {
    fetchAllMedia("all");
    fetchAllTrendingTopics();
  }, [selectedCategoryId]);

  const handleChange = (event) => {
    setSelectedCategoryId(event.target.value);
  };
  const [modalOpen, setModalOpen] = useState(false);
  const [details, setDetails] = useState({});
  const handleOpenArchivesModal = (img, index) => {
    setModalOpen(true);
    setDetails(img);
  };

  const handleChangeToArchives = async () => {
    setArchiveLoading(true);
    try {
      const payload = { id: details?.id };
      const response = await changeMediaToArchive(payload);
      if (response?.status === true) {
        toast.success(response?.message || "");
      }
    } catch (error) {
      toast.error("Something went wrong");
    } finally {
      fetchAllMedia("all");
      setModalOpen(false);
      setArchiveLoading(false);
    }
  };

  const handleClose = () => {
    setModalOpen(false);
    setDetails({});
  };

  const deleteClose = () => {
    setOpen1(false);
  };

  const onConfirm = async () => {
    setDeleteLoading(true);
    try {
      const response = await deleteMediaById(singleData?.id);
      if (response?.status === true) {
        toast.success("Image deleted successfully!");
        fetchAllMedia("all");
        setOpen1(false);
      }
    } catch (error) {
      console.log(error, "error");
    } finally {
      setDeleteLoading(false);
    }
  };
  const handleOpenDeleteModal = (data) => {
    setSingleData(data);
    setOpen1(true);
  };

  const fetchAllArchived = () => {
    fetchAllMedia("archived");
    setArchived(true);
  };

    const [restore, setRestore] = useState(false);
    const handleRestoreFromArchives = async () => {
      setRestoreLoading(true);
      try {
        const payload = { id: details?.id };
        const response = await restoreMedia(payload);
        if (response?.status === true) {
          toast.success(response?.message || "");
        }
      } catch (error) {
        toast.error("Something went wrong");
      } finally {
        fetchAllMedia("all");
        setRestore(false);
        setRestoreLoading(false);
      }
    };
  
    const handleOpenRestoreModal = (img, index) => {
      setRestore(true);
      setDetails(img);
    };

  return (
    <>
      <Dialog
        open={restore}
        onClose={handleClose}
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
          onClick={handleClose}
          sx={{ position: "absolute", top: 12, right: 12 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>

        {/* Title */}
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "24px",
            p: 0,
            mb: 1,
          }}
        >
          Are you sure ?
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
            you want to restore from archives
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
            onClick={handleClose}
            disabled={restoreLoading}
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
            onClick={handleRestoreFromArchives}
            disabled={restoreLoading}
            sx={{
              backgroundColor: "#0047AB",
              color: "#fff",
              borderRadius: "8px",
              textTransform: "none",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#002f8c",
              },
            }}
          >
            {restoreLoading ? "Restoring..." : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={open1}
        onClose={deleteClose}
        PaperProps={{
          sx: {
            borderRadius: "16px",
            padding: "20px",
            maxWidth: "420px",
            width: "100%",
          },
        }}
      >
        {/* Close Icon */}
        <IconButton
          onClick={deleteClose}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
          }}
        >
          <CloseIcon />
        </IconButton>
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#484848",
            fontSize: "20px",
            p: 0,
            mb: 1,
          }}
        >
          Delete?
        </DialogTitle>
        <DialogContent sx={{ padding: "20px 5px" }}>
          <Typography
            sx={{
              fontSize: "16px",
              color: "#616161",
              mb: 3,
              textAlign: "start",
            }}
          >
            Are you sure you want to delete this image?
          </Typography>

          {/* Buttons */}
          <Box display="flex" justifyContent="end" gap={2}>
            <Button
              onClick={deleteClose}
              disabled={deleteLoading}
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
              onClick={onConfirm}
              disabled={deleteLoading}
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
              {deleteLoading ? "Deleting..." : "Confirm"}
            </Button>
          </Box>
        </DialogContent>
      </Dialog>

      <Dialog
        open={modalOpen}
        onClose={handleClose}
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
          onClick={handleClose}
          sx={{ position: "absolute", top: 12, right: 12 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>

        {/* Title */}
        <DialogTitle
          sx={{
            fontWeight: "500",
            color: "#3B3B3B",
            fontSize: "24px",
            p: 0,
            mb: 1,
          }}
        >
          Are you sure ?
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
            you want to move on archives
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
            onClick={handleClose}
            disabled={archiveLoading}
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
            onClick={handleChangeToArchives}
            disabled={archiveLoading}
            // variant="contained"
            sx={{
              backgroundColor: "#0047AB",
              color: "#fff",
              borderRadius: "8px",
              textTransform: "none",
              px: 4,
              py: 1,
              fontWeight: 500,
              fontSize: "16px",
              "&:hover": {
                backgroundColor: "#002f8c",
              },
            }}
          >
            {archiveLoading ? "Archiving..." : "Confirm"}
          </Button>
        </DialogActions>
      </Dialog>
      <Card
        className="settings-container"
        elevation={0}
        sx={{ px: 2, py: 3, borderRadius: 4 }}
      >
        <Box
          display="flex"
          justifyContent="space-between"
          alignItems="center"
          mb={3}
          flexWrap="wrap"
          gap={1}
        >
          <Typography
            variant="h6"
            color="#3B3B3B"
            sx={{
              fontSize: 15,
            }}
            fontWeight={500}
          >
            {archived ? "Archeives" : "Images"} (
            <span style={{ color: "#2D76DC" }}>{totalMedia?.length}</span>)
          </Typography>

          <Box display="flex" flexWrap="wrap" gap={1} alignItems="center">
            <Box display="flex" alignItems="center" gap={1}>
              <Typography
                variant="body2"
                sx={{
                  color: "#95919D",
                  fontWeight: 400,
                  fontSize: 13,
                }}
              >
                Category:
              </Typography>

              <FormControl size="small">
                <Select
                  defaultValue="all"
                  displayEmpty
                  value={selectedCategoryId}
                  onChange={handleChange}
                  variant="outlined"
                  sx={{
                    borderRadius: "8px",
                    backgroundColor: "#fff",
                    minWidth: 146,
                    height: 33,
                    fontWeight: 500,
                    fontSize: 13,
                    color: "#111827", // Black text
                    "& .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#E5E7EB", // Light border
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#2563EB", // Blue focus
                    },
                    "& .MuiSelect-icon": {
                      color: "#2563EB", // Blue arrow
                    },
                  }}
                  MenuProps={{
                    PaperProps: {
                      style: {
                        maxHeight: 88 * 4.5 + 8,
                        width: 250,
                      },
                    },
                  }}
                >
                  <MenuItem value="all">All Category</MenuItem>
                  {categoriesData?.map((category) => (
                    <MenuItem key={category.id} value={category.id}>
                      {category?.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>

            <Box display="flex" alignItems="center" gap={1}>
              <Typography
                variant="body2"
                sx={{
                  color: "#95919D",
                  fontWeight: 400,
                  fontSize: 13,
                }}
              >
                Sort By:
              </Typography>

              <FormControl size="small">
                <Select
                  defaultValue="Recently Added"
                  displayEmpty
                  variant="outlined"
                  sx={{
                    borderRadius: "8px",
                    backgroundColor: "#fff",
                    minWidth: 148,
                    height: 33,
                    fontWeight: 500,
                    fontSize: 13,
                    color: "#111827", // Black text
                    "& .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#E5E7EB", // Light border
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#2563EB", // Blue focus
                    },
                    "& .MuiSelect-icon": {
                      color: "#2563EB", // Blue arrow
                    },
                  }}
                >
                  <MenuItem value="Recently Added">Recently Added</MenuItem>
                  <MenuItem value="Oldest">Oldest</MenuItem>
                </Select>
              </FormControl>
            </Box>
            {!archived && (
              <Box
                display={"flex"}
                bgcolor={"#F4F0FF"}
                borderRadius={1}
                padding={"7px 9px"}
                alignItems="center"
                gap={1}
                sx={{
                  cursor: "pointer",
                }}
                onClick={fetchAllArchived}
              >
                <img
                  src={archives}
                  alt="Archive"
                  width="18px"
                  height={"18px"}
                />
                <Typography fontSize={"13px"} variant="body1" color="#3B3B3B">
                  Archives
                </Typography>
              </Box>
            )}

            {!archived && (
              <Box
                display={"flex"}
                bgcolor={"#F4F0FF"}
                borderRadius={1}
                padding={"7px 9px"}
                alignItems="center"
                gap={1}
                sx={{
                  cursor: "pointer",
                }}
                onClick={handleBulkUpload}
              >
                <img src={bulk} alt="Bulk" width="16px" height={"16px"} />
                <Typography fontSize={"13px"} variant="body1" color="#3B3B3B">
                  {" "}
                  Bulk Upload
                </Typography>
              </Box>
            )}

            {!archived && (
              <Button
                sx={{
                  bgcolor: "#0047AB",
                  color: "#fff",
                  fontSize: "13px",
                  fontWeight: "400",
                  padding: "5px 10px !important",
                  textTransform: "none",
                  px: 3,
                  "&:hover": {
                    bgcolor: "#1E40AF",
                  },
                }}
                onClick={handleOpen}
              >
                + Image
              </Button>
            )}
          </Box>
        </Box>
        <Box
          className="scroll-container"
          sx={{ height: "calc(100vh - 64px)", overflowY: "auto", pr: 1 }}
        >
          {totalMedia?.length > 0 ? (
            <Box
              sx={{
                columnCount: { xs: 1, sm: 2, md: 2, lg: 3 },
                columnGap: "16px",
              }}
            >
              {totalMedia?.map((img, index) => (
                <Box
                  key={index}
                  sx={{
                    mb: 2,
                    breakInside: "avoid",
                    background: "#fff",
                    borderRadius: 3,
                    overflow: "hidden",
                    boxShadow: "0 4px 15px rgba(0,0,0,0.1)",
                    cursor: "pointer",
                    transition: "transform 0.3s ease, box-shadow 0.3s ease",
                    "&:hover": {
                      transform: "translateY(-5px)",
                      boxShadow: "0 8px 25px rgba(0,0,0,0.15)",
                    },
                  }}
                >
                  {/* Image */}
                  <Box
                    sx={{
                      position: "relative",
                      height: 320,
                      backgroundImage: `url(${img?.url || img1})`,
                      backgroundSize: "cover",
                      backgroundPosition: "center",
                    }}
                    onClick={() => handleCardClick(index)}
                  >
                    {img?.isArchived === true ? (
                      <Box
                        borderRadius={1}
                        padding={"6px 10px"}
                        sx={{
                          position: "absolute",
                          top: 10,
                          right: 10,
                          // "&:hover": {
                          bgcolor: "#F4F0FF",
                          cursor: "pointer",
                          color: "#3B3B3B",
                          // },
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenRestoreModal(img, index);
                        }}
                      >
                        <Typography
                          display={"flex"}
                          variant="body1"
                          // color="#3B3B3B"
                          color="#fff"
                          sx={{
                            // "&:hover": {
                            color: "#3B3B3B",
                            // },
                            fontSize: "12px",
                          }}
                        >
                          <img src={archives} alt="Archive" width="18px" />{" "}
                          Archived
                        </Typography>
                      </Box>
                    ) : (
                      <Box
                        borderRadius={1}
                        padding={"6px 10px"}
                        sx={{
                          position: "absolute",
                          top: 10,
                          right: 10,
                          "&:hover": {
                            bgcolor: "#F4F0FF",
                            cursor: "pointer",
                            color: "#3B3B3B",
                          },
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenArchivesModal(img, index);
                        }}
                      >
                        <Typography
                          display={"flex"}
                          variant="body1"
                          // color="#3B3B3B"
                          color="#fff"
                          sx={{
                            "&:hover": {
                              color: "#3B3B3B",
                            },
                            fontSize: "12px",
                          }}
                        >
                          <img src={archives} alt="Archive" width="18px" />{" "}
                          Archive
                        </Typography>
                      </Box>
                    )}
                  </Box>

                  {/* Expand Content */}
                  <Collapse
                    in={expandedIndex === index}
                    timeout="auto"
                    unmountOnExit
                  >
                    <Box bgcolor={"#F8F9FC"} sx={{ p: 2 }}>
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                        }}
                      >
                        <Typography variant="h6" color="#3F3D3D">
                          {img?.title || "Image Title"}
                        </Typography>
                        <Typography variant="body2" color="#95919D">
                          {Math.floor(Math.random() * 900 + 100)} KB
                        </Typography>
                      </Box>
                      <Typography
                        variant="subtitle2"
                        color="#95919D"
                        mb={1}
                        mt={1.5}
                      >
                        Categories:
                      </Typography>
                      {img?.categories?.map((category, index) => (
                        <Button
                          key={index}
                          size="small"
                          sx={{
                            border: "1px solid #2D76DC",
                            bgcolor: "#E7F1FF",
                            color: "#000000",
                            textTransform: "capitalize",
                            padding: "5px 10px",
                          }}
                        >
                          {category}
                        </Button>
                      ))}

                      <Typography
                        variant="subtitle2"
                        color="#95919D"
                        mb={1}
                        mt={1.5}
                      >
                        Tags:
                      </Typography>
                      <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                        {img?.hashtags?.map((tag, index) => (
                          <Button
                            key={index}
                            size="small"
                            sx={{
                              border: "1px solid #2D76DC",
                              bgcolor: "#E7F1FF",
                              color: "#000000",
                              textTransform: "capitalize",
                              padding: "5px 10px",
                            }}
                          >
                            <img
                              style={{ width: 14, height: 14, marginRight: 2 }}
                              src={tagIcon}
                              alt="img"
                            />{" "}
                            {tag}
                          </Button>
                        ))}
                      </Box>

                      <Box mt={2} sx={{ display: "flex", gap: 1 }}>
                        {/* <Button
                          sx={{
                            bgcolor: "#FFFFFF",
                            color: "#3B3B3B",
                            textTransform: "capitalize",
                            padding: "5px 10px",
                            width: "100%",
                            fontSize: "15px",
                          }}
                        >
                          Edit
                        </Button> */}
                        <Button
                          sx={{
                            bgcolor: "#FFFFFF",
                            color: "#CB2E1C",
                            textTransform: "capitalize",
                            padding: "5px 10px",
                            width: "100%",
                            fontSize: "15px",
                          }}
                          onClick={() => handleOpenDeleteModal(img)}
                        >
                          Delete
                        </Button>
                      </Box>
                    </Box>
                  </Collapse>
                </Box>
              ))}
            </Box>
          ) : (
            <Typography textAlign={"center"} variant="body1" color="#95919D">
              No data found
            </Typography>
          )}
        </Box>
        <Dialog
          open={open}
          onClose={onClose}
          fullWidth
          PaperProps={{
            sx: {
              width: "600px",
              borderRadius: "16px",
              overflow: "hidden",
            },
          }}
          maxWidth="md"
          fullScreen={isMobile}
        >
          <DialogTitle
            sx={{
              fontWeight: 600,
              fontSize: "16px",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              color: "#484848",
            }}
          >
            Add an image to library
            <IconButton onClick={onClose}>
              <CloseIcon />
            </IconButton>
          </DialogTitle>

          <DialogContent>
            {/* Upload/Preview Area */}
            <Box
              sx={{
                border: image ? "none" : "2px dashed #E0E0E0",
                backgroundColor: "#F8F9FC",
                borderRadius: "8px",
                height: "250px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 2,
                mb: 3,
                overflow: "hidden",
              }}
              onDrop={handleDrop}
              onDragOver={(e) => e.preventDefault()}
            >
              {image ? (
                <>
                  <Box
                    sx={{
                      width: "25%",
                      height: "100%",
                      objectFit: "cover",
                      borderRadius: "8px",
                    }}
                  />
                  <Box
                    component="img"
                    src={image}
                    alt="preview"
                    sx={{
                      width: "50%",
                      height: "100%",
                      objectFit: "cover",
                      borderRadius: "8px",
                    }}
                  />
                  <Box
                    sx={{
                      flex: 1,
                      height: "100%",
                      display: "flex",
                      justifyContent: "end",
                      alignItems: "end",
                      padding: "0px 15px 15px 0px",
                      cursor: "pointer",
                    }}
                  >
                    <Box display={"flex"} alignItems={"center"} gap={1}>
                      <img src={deleteicon} width={17} height={17} />
                      <Typography
                        onClick={() => setImage(null)}
                        sx={{ textTransform: "none", color: "#3B3B3B" }}
                      >
                        Delete
                      </Typography>
                    </Box>
                  </Box>
                </>
              ) : (
                <label
                  htmlFor="fileInput"
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    cursor: "pointer",
                  }}
                >
                  {loader ? (
                    <Box
                      sx={{
                        width: "100%",
                        height: "100%",
                        display: "flex",
                        justifyContent: "center",
                        alignItems: "center",
                      }}
                    >
                      <CircularProgress />
                    </Box>
                  ) : (
                    <>
                      <input
                        id="fileInput"
                        type="file"
                        accept="image/*"
                        style={{ display: "none" }}
                        onChange={handleBrowse}
                      />
                      <img
                        src={UploadIcon}
                        alt="upload"
                        style={{ width: 40, height: 40, opacity: 0.6 }}
                      />
                      <Typography variant="body2" color="#121212">
                        Drop your images here, or{" "}
                        <span style={{ color: "#2D76DC" }}>browse</span>
                      </Typography>
                      <Typography
                        variant="caption"
                        color="#95919D"
                        sx={{ fontSize: "10px" }}
                      >
                        Supports PNG, JPEG & WEBP up to 40MB
                      </Typography>
                    </>
                  )}
                </label>
              )}
            </Box>

            {/* Form Fields */}
            <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
              <Box
                width={"50%"}
                sx={{
                  display: "flex",
                  height: 45,
                  flexDirection: "column",
                  gap: 1,
                }}
              >
                <Typography
                  sx={{
                    fontSize: "13px",
                    fontWeight: 400,
                    color: "#95919D",
                  }}
                >
                  Title
                </Typography>
                <TextField
                  sx={{
                    "& .MuiInputBase-root": {
                      height: 45, // total height of the input box
                    },
                  }}
                  placeholder="Enter Title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  fullWidth
                />
              </Box>
              <Box
                width={"50%"}
                sx={{ display: "flex", flexDirection: "column", gap: 1 }}
              >
                <Typography
                  sx={{
                    fontSize: "13px",
                    fontWeight: 400,
                    color: "#95919D",
                  }}
                >
                  Category
                </Typography>
                <Select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  fullWidth
                  sx={{ height: 45 }}
                  MenuProps={{
                    PaperProps: {
                      style: {
                        maxHeight: 48 * 4.5 + 8,
                        width: 250,
                      },
                    },
                  }}
                >
                  <MenuItem value="" disabled>
                    Select Category
                  </MenuItem>
                  {categoriesData?.map((cat) => (
                    <MenuItem key={cat?.id} value={cat?.id}>
                      {cat?.name}
                    </MenuItem>
                  ))}
                </Select>
              </Box>
            </Box>
            <Box
              width={"49%"}
              sx={{ display: "flex", flexDirection: "column", gap: 1 }}
            >
              <Autocomplete
                freeSolo
                options={trendsData.filter(
                  (trend) =>
                    trend.toLowerCase().includes(trendsInput.toLowerCase()) &&
                    !selectedTrends.includes(trend)
                )}
                inputValue={trendsInput}
                onInputChange={(event, newInputValue, reason) => {
                  if (reason === "input") {
                    setTrendsInput(newInputValue);
                  }
                }}
                onChange={(event, newValue) => addTrends(newValue)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    variant="standard"
                    sx={{
                      color: "#E0E0E0",
                      "& .MuiInputBase-input": {
                        fontSize: "12px",
                      },
                      "& .MuiFormHelperText-root": {
                        fontSize: "10px",
                        marginTop: "2px",
                      },
                    }}
                    placeholder="Type and press Enter to add"
                    helperText="Press Enter to add. No # needed. Paste comma-separated tags to add many at once."
                  />
                )}
                PopperComponent={(props) => (
                  <Popper
                    {...props}
                    sx={{
                      width: "200px",
                      padding: "8px",
                      "& .MuiAutocomplete-listbox": {
                        padding: "5px",
                        border: "1px solid #DEDFE5",
                        borderRadius: "8px",
                        "& li": {
                          backgroundColor: "#FFFFFF",
                          padding: "8px",
                          borderRadius: "8px",
                          cursor: "pointer",
                          transition: "background-color 0.3s ease",
                        },
                        "& li:hover": {
                          backgroundColor: "#0047AB",
                          color: "#FFFFFF",
                        },
                      },
                    }}
                  />
                )}
                getOptionLabel={(option) =>
                  typeof option === "string" ? option : option || ""
                }
                disableClearable
              />
              {selectedTrends?.length > 0 && (
                <Typography sx={{ fontSize: "11px", color: "#95919D", mt: 0.5 }}>
                  {selectedTrends.length} hashtag{selectedTrends.length === 1 ? "" : "s"} added
                </Typography>
              )}
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                {selectedTrends?.map((trend, index) => (
                  <Chip
                    key={index}
                    label={trend}
                    onDelete={() => removeTrends(trend)}
                    deleteIcon={<CloseIcon fontSize="10px" />}
                    sx={{
                      backgroundColor: "#E7F1FF",
                      border: "1px solid #2D76DC",
                      color: "#2D76DC",
                      fontWeight: 400,
                      fontSize: "10px",
                      height: "24px",
                    }}
                  />
                ))}
              </Box>
            </Box>

            {/* Buttons */}
            <Box
              sx={{
                display: "flex",
                justifyContent: "flex-end",
                gap: 2,
                mt: 4,
              }}
            >
              <Button
                onClick={onClose}
                sx={{
                  backgroundColor: "#F4F0FF",
                  color: "#0047AB",
                  textTransform: "none",
                  borderRadius: "8px",
                  px: 3,
                  "&:hover": {
                    backgroundColor: "#EDE8FF",
                  },
                }}
              >
                Cancel
              </Button>
              {loader ? (
                <Box>
                  <CircularProgress />
                </Box>
              ) : (
                <Button
                  onClick={handleConfirm}
                  variant="contained"
                  sx={{
                    backgroundColor: "#0047AB",
                    textTransform: "none",
                    borderRadius: "8px",
                    px: 3,
                    "&:hover": {
                      backgroundColor: "#001F5F",
                    },
                  }}
                >
                  Confirm
                </Button>
              )}
            </Box>
          </DialogContent>
        </Dialog>

        <Dialog
          open={bulkUpload}
          onClose={onClose}
          fullWidth
          maxWidth="md"
          fullScreen={isMobile}
        >
          <DialogTitle
            sx={{
              fontWeight: 600,
              fontSize: "16px",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              color: "#484848",
            }}
          >
            Bulk Upload
            <IconButton onClick={onClose}>
              <CloseIcon />
            </IconButton>
          </DialogTitle>
          <DialogContent sx={{ p: 3 }}>
            {/* Upload Box */}
            <Box
              sx={{
                border: "1px dashed #D9E1EC",
                borderRadius: "8px",
                background: "#F8FAFC",
                p: 2,
                textAlign: "center",
                mb: 3,
              }}
            >
              {!file ? (
                <>
                  <CloudUploadIcon sx={{ fontSize: 40, color: "#F97316" }} />
                  <Typography variant="body2" sx={{ mt: 1 }}>
                    Upload Zip File
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Max size 250 MB. Only ZIP files are supported
                  </Typography>
                  <input
                    type="file"
                    accept=".zip"
                    onChange={handleZipFileUpload}
                    style={{ display: "none" }}
                    id="upload-zip"
                  />
                  <label htmlFor="upload-zip">
                    <Button component="span" sx={{ mt: 1 }}>
                      Browse
                    </Button>
                  </label>
                </>
              ) : (
                <Box sx={{ textAlign: "left" }}>
                  <Box sx={{ display: "flex", alignItems: "center" }}>
                    <Typography sx={{ flexGrow: 1 }}>{file.name}</Typography>
                  </Box>
                  <Box display="flex" gap={2} alignItems="center">
                    <LinearProgress
                      variant="determinate"
                      value={progress}
                      sx={{ height: 10, width: "95%", borderRadius: "4px" }}
                    />
                    <Typography variant="body2" sx={{ minWidth: 35 }}>
                      {progress}%
                    </Typography>
                    <IconButton size="small" onClick={handleDeleteFile}>
                      <img src={deleteicon} width={20} height={20} />
                    </IconButton>
                  </Box>
                  <Typography variant="caption">
                    Total Items:{" "}
                    <span style={{ color: "#2D76DC" }}>{totalItems}</span> |
                    Success:{" "}
                    <span style={{ color: "#1CCE0F" }}>{successCount}</span> |
                    Failed:{" "}
                    <span style={{ color: "#DB3B13" }}>{failedCount}</span>
                  </Typography>
                </Box>
              )}
            </Box>
            <Typography mt={2} fontSize={"16px"} mb={2}>
              Choose For All
            </Typography>
            <Box sx={{ display: "flex", gap: 2, mb: 1 }}>
              <Box width={"50%"}>
                <Typography
                  sx={{
                    fontSize: "14px",
                    fontWeight: 400,
                    color: "#95919D",
                  }}
                  mb={0.5}
                >
                  Category
                </Typography>
                <TextField
                  select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  fullWidth
                  // variant="standard"
                  InputLabelProps={{
                    sx: {
                      color: "#ccc",
                      fontSize: "16px",
                    },
                  }}
                  SelectProps={{
                    IconComponent: () => (
                      <svg
                        width="30px"
                        height="25px"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path d="M7 10l5 5 5-5H7z" fill="#2D76DC" />
                      </svg>
                    ),
                    sx: {
                      paddingTop: "4px",
                    },
                  }}
                  sx={{
                    "& .MuiInputBase-root": {
                      height: 45, // total height of the input box
                    },
                    "& .MuiInput-underline:before": {
                      borderBottom: "1px solid #E0E0E0",
                    },
                    "& .MuiInput-underline:hover:not(.Mui-disabled):before": {
                      borderBottom: "1px solid #E0E0E0",
                    },
                    "& .MuiInput-underline:after": {
                      borderBottom: "1px solid #E0E0E0",
                    },
                    "& .MuiSelect-select": {
                      padding: "14.5px 14px", // top-bottom, left-right
                    },
                  }}
                >
                  {categoriesData?.map((cat) => (
                    <MenuItem key={cat?.id} value={cat?.id}>
                      {cat?.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Box>
              <Box
                width={"49%"}
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "end",
                  gap: 1,
                }}
              >
                <Autocomplete
                  freeSolo
                  options={trendsData.filter(
                    (trend) =>
                      trend.toLowerCase().includes(trendsInput.toLowerCase()) &&
                      !selectedTrends.includes(trend)
                  )}
                  inputValue={trendsInput}
                  onInputChange={(event, newInputValue, reason) => {
                    if (reason === "input") {
                      setTrendsInput(newInputValue);
                    }
                  }}
                  onChange={(event, newValue) => addTrends(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      variant="standard"
                      sx={{
                        color: "#E0E0E0",
                        "& .MuiInputBase-input": {
                          fontSize: "12px",
                        },
                      }}
                      placeholder="Trending Topics"
                      onKeyDown={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          addTrends(trendsInput);
                        }
                      }}
                    />
                  )}
                  PopperComponent={(props) => (
                    <Popper
                      {...props}
                      sx={{
                        width: "200px",
                        padding: "8px",
                        "& .MuiAutocomplete-listbox": {
                          padding: "5px",
                          border: "1px solid #DEDFE5",
                          borderRadius: "8px",
                          "& li": {
                            backgroundColor: "#FFFFFF",
                            padding: "8px",
                            borderRadius: "8px",
                            cursor: "pointer",
                            transition: "background-color 0.3s ease",
                          },
                          "& li:hover": {
                            backgroundColor: "#0047AB",
                            color: "#FFFFFF",
                          },
                        },
                      }}
                    />
                  )}
                  getOptionLabel={(option) =>
                    typeof option === "string" ? option : option || ""
                  }
                  disableClearable
                />
                <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                  {selectedTrends?.map((trend, index) => (
                    <Chip
                      key={index}
                      label={trend}
                      onDelete={() => removeTrends(trend)}
                      deleteIcon={<CloseIcon fontSize="10px" />}
                      sx={{
                        backgroundColor: "#E7F1FF",
                        border: "1px solid #2D76DC",
                        color: "#2D76DC",
                        fontWeight: 400,
                        fontSize: "10px",
                        height: "24px",
                      }}
                    />
                  ))}
                </Box>
              </Box>
            </Box>

            {/* Actions */}
            <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 2 }}>
              <Button
                onClick={onClose}
                sx={{
                  backgroundColor: "#F4F0FF",
                  color: "#0047AB",
                  textTransform: "none",
                  borderRadius: "8px",
                  px: 3,
                  "&:hover": {
                    backgroundColor: "#EDE8FF",
                  },
                }}
              >
                Cancel
              </Button>
              <Button
                onClick={handleZipConfirm}
                sx={{
                  backgroundColor: "#0047AB",
                  textTransform: "none",
                  borderRadius: "8px",
                  color: "#fff",
                  px: 3,
                  "&:hover": {
                    backgroundColor: "#001F5F",
                  },
                }}
              >
                Confirm
              </Button>
            </Box>
          </DialogContent>
        </Dialog>
      </Card>
    </>
  );
};

export default ImageManagement;
