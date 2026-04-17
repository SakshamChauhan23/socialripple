import { Box, Typography, Tooltip, useMediaQuery } from "@mui/material";
import { useTheme } from "@mui/material/styles";

const CategoryListing = ({ postReviewData }) => {
  const categories = postReviewData?.preparedPost?.categories || [];
  const theme = useTheme();
  const isTablet = useMediaQuery(theme.breakpoints.down("md")); // 1080px or below

  // show only 3 on desktop, 2 on tablet/mobile
  const visibleCount = isTablet ? 2 : 3;
  const visibleCategories = categories.slice(0, visibleCount);
  const hiddenCategories = categories.slice(visibleCount);

  return (
    <Box display="flex" gap={1} flexWrap="wrap" alignItems="center">
      {visibleCategories.map((category) => (
        <Box
          key={category.id}
          bgcolor={"#E7F1FF"}
          borderRadius={"4px"}
          padding={"6px 8px"}
          border={"0.6px solid #2D76DC"}
        >
          <Typography fontSize={"10px"} fontWeight={400}>
            {category?.categoryName}
          </Typography>
        </Box>
      ))}

      {/* Show ... with tooltip for remaining categories */}
      {hiddenCategories.length > 0 && (
        <Tooltip
          title={
            <Box>
              {hiddenCategories.map((cat) => (
                <Typography key={cat.id} fontSize={"10px"}>
                  {cat.categoryName}
                </Typography>
              ))}
            </Box>
          }
          arrow
          placement="top"
        >
          <Typography
            fontSize={"12px"}
            fontWeight={500}
            color="#2D76DC"
            sx={{ cursor: "pointer" }}
          >
            ...
          </Typography>
        </Tooltip>
      )}
    </Box>
  );
};

export default CategoryListing;
