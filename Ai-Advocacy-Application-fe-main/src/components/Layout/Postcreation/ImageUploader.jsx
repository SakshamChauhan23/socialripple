import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  TextField,
  InputBase,
  MenuItem,
  Grid,
  Box,
  Typography,
  Button,
  Checkbox,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import { useEffect, useState } from "react";
import SearchIcon from "@mui/icons-material/Search";
import img3 from "../../../assets/timeline/pic3.png";
import { getAllMediaByType } from "../../../services/postService";
import { Auth } from "../../../contexts/AuthContext";

const ImageLibraryModal = ({ open, onClose, onConfirm }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const [searchTerm, setSearchTerm] = useState("");
  const categoriesList = Auth();
  const [category, setCategory] = useState();
  const [selected, setSelected] = useState([]);
  const [uploadedImages, setUploadedImages] = useState([]);
  const [totalMedia, setTotalMedia] = useState([]);
  const toggleSelect = (img) => {
    setSelected((prev) =>
      prev.includes(img)
        ? prev.filter((i) => i !== img)
        : prev.length < 6
        ? [...prev, img]
        : prev
    );
  };
  const fetchAllMedia = async () => {
    try {
      const payload = {
        type: "image",
        categoryId: category,
        search: searchTerm,
      };
      const response = await getAllMediaByType(payload);
      if (response?.status === true) {
        setTotalMedia(response?.mediaLibrary);
      }
    } catch (error) {
      console.log(error);
    }
  };

  useEffect(() => {
    fetchAllMedia();
  }, [category]);

  const handleDeviceUpload = (e) => {
    const files = Array.from(e.target.files);
    const newImageObjects = files.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
    }));
    setUploadedImages((prev) => [...prev, ...newImageObjects]);
    setSelected((prev) => [
      ...prev,
      ...newImageObjects.map((img) => img.preview),
    ]);
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="md"
     
    
      fullScreen={isMobile}
    >
      <DialogTitle sx={{ fontWeight: 500, fontSize: 18, color: "#3B3B3B" }}>
        Select An Image From Library
        <IconButton
          onClick={onClose}
          sx={{ position: "absolute", right: 16, top: 16 }}
        >
          <CloseIcon sx={{ color: "#000" }} />
        </IconButton>
      </DialogTitle>

      <DialogContent>
        <Box
          display="flex"
          gap={2}
          width={"100%"}
          alignItems="flex-start"
          mb={3}
        >
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              backgroundColor: "#f7f8fa",
              border: "1px solid #e0e0e0",
              borderRadius: "6px",
              px: 2,
              height: 40,
              width: "50%",
              mt: 3.3,
            }}
          >
            <SearchIcon sx={{ color: "#1976d2", mr: 1 }} />
            <InputBase
              placeholder="Search"
              fullWidth
              onChange={(e) => setSearchTerm(e.target.value)}
              sx={{ color: "#666", fontSize: "16px" }}
            />
          </Box>

          {/* Category Dropdown */}
          <Box width={"50%"}>
            <Typography
              variant="body2"
              sx={{ color: "#9e9e9e", mb: "6px", ml: "2px" }}
            >
              Category
            </Typography>
            <TextField
              select
              size="small"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              sx={{
                width: "100%",
                height: "50px",
                backgroundColor: "#",
                "& .MuiOutlinedInput-root": {
                  borderRadius: "8px",
                  border: "1px solid #DAE1EB",
                },
                "& .MuiInputBase-input": {
                  color: "#000000",
                  background: "#FCFCFC",
                  padding: "8.8px 14px",
                },
                "& .MuiSvgIcon-root": {
                  color: "#2D76DC",
                },
              }}
              MenuProps={{
                PaperProps: {
                  style: {
                    maxHeight: 88 * 4.5 + 8,
                    width: '100%',
                  },
                },
                anchorOrigin: {
                  vertical: "bottom",
                  horizontal: "left",
                },
                transformOrigin: {
                  vertical: "top",
                  horizontal: "left",
                },
                getContentAnchorEl: null, // Ensures the menu anchors below the select
              }}
            >
              <MenuItem value="">All Category</MenuItem>
              {categoriesList?.categoriesData?.map((cat) => (
                <MenuItem key={cat?.id} value={cat?.id}>
                  {cat?.name}
                </MenuItem>
              ))}
            </TextField>
          </Box>
        </Box>
        <Box display="flex" justifyContent={"end"}>
          <Typography
            variant="body2"
            sx={{ color: "#9e9e9e", mb: "6px", ml: "2px" }}
          >
            Selected Images {`${selected.length}/${totalMedia.length}`}
          </Typography>
        </Box>
        <Box sx={{ maxHeight: 400, overflowY: "auto", p: 1 }}>
          <Grid container spacing={2}>
            {totalMedia?.map((img) => (
              <Grid item xs={4} key={img?.id}>
                <Box
                  onClick={() => toggleSelect(img)}
                  sx={{
                    position: "relative",
                    borderRadius: 2,
                    overflow: "hidden",
                    border: selected.includes(img)
                      ? "2px solid #0047AB"
                      : "2px solid transparent",
                    cursor: "pointer",
                  }}
                >
                  <img
                    src={img?.url || img3}
                    alt=""
                    style={{
                      width: "100%",
                      height: "150px",
                      objectFit: "cover",
                    }}
                  />
                  {selected.includes(img) && (
                    <Checkbox
                      checked
                      sx={{
                        position: "absolute",
                        top: 8,
                        left: 8,
                        // background: "white",
                        borderRadius: "50%",
                      }}
                    />
                  )}
                </Box>
              </Grid>
            ))}
          </Grid>
        </Box>

        <Box mt={3} display="flex" justifyContent="space-between">
          <Button
            variant="outlined"
            sx={{
              backgroundColor: "#F4F0FF",
              color: "#3B3B3B",
              textTransform: "none",
              borderRadius: 2,
              px: 3,
              fontWeight: 500,
              border: "none",
            }}
            onClick={() => {
              const input = document.getElementById("modal-device-upload");
              if (input) {
                input.click();
              } else {
                console.warn("Upload input not found");
              }
            }}
          >
            Upload from Device
          </Button>

          <Box display="flex" gap={2}>
            <Button
              onClick={onClose}
              sx={{
                backgroundColor: "#f6f1ff",
                color: "#0047AB",
                textTransform: "none",
                borderRadius: 2,
                px: 4,
                fontWeight: 500,
              }}
            >
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={() => {
                onConfirm(selected);
                onClose();
              }}
              sx={{
                backgroundColor: "#0047AB",
                textTransform: "none",
                borderRadius: 2,
                px: 4,
              }}
            >
              Confirm
            </Button>
          </Box>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default ImageLibraryModal;
