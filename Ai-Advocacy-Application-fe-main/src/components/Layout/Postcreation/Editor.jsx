

// import React, { useState, useRef } from "react";
// import {
//   Box,
//   Typography,
//   IconButton,
//   Button,
//   Snackbar,
//   Alert,
//   Checkbox,
// } from "@mui/material";
// import FormatBoldIcon from "@mui/icons-material/FormatBold";
// import FormatItalicIcon from "@mui/icons-material/FormatItalic";
// import FormatUnderlinedIcon from "@mui/icons-material/FormatUnderlined";
// import FormatListBulletedIcon from "@mui/icons-material/FormatListBulleted";
// import ContentCopyIcon from "@mui/icons-material/ContentCopy";
// import "react-quill/dist/quill.snow.css";
// import ReactQuill from "react-quill";


// const EditorBox = ({ value, onChange }) => {
//   const [copiedText, setCopiedText] = useState("");
//   const [showToast, setShowToast] = useState(false);
//   const quillRef = useRef(null);

//   const handleFormat = (format, val = true) => {
//     const editor = quillRef.current?.getEditor();
//     if (editor) {
//       const range = editor.getSelection();
//       if (range) {
//         editor.format(format, val);
//       }
//     }
//   };

//   const handleCopy = () => {
//     const tempEl = document.createElement("div");
//     tempEl.innerHTML = value;
//     const plainText = tempEl.innerText;
//     navigator.clipboard.writeText(plainText);
//     setCopiedText(plainText);
//     setShowToast(true);
//   };

//   return (
//     <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
//       <Box
//         sx={{
//           flex: 1,
//           display: "flex",
//           flexDirection: "column",
//           border: "1px solid #e0e0e0",
//           borderRadius: "8px",
//           backgroundColor: "#fafafa",
//           overflow: "hidden",
//         }}
//       >
//         {/* Editor */}
//         <Box
//           sx={{
//             flex: 1,
//             px: 2,
//             pt: 2,
//             "& .ql-container": { border: "none !important" },
//             "& .ql-editor": { minHeight: "150px" },
//           }}
//         >
//           <ReactQuill
//             ref={quillRef}
//             value={value}
//             onChange={onChange}   // 👈 parent se update
//             modules={{ toolbar: false }}
//             style={{
//               height: "100%",
//               backgroundColor: "#fafafa",
//               border: "none",
//               fontSize: "16px",
//             }}
//           />
//         </Box>

//         {/* Toolbar */}
//         <Box
//           sx={{
//             display: "flex",
//             justifyContent: "space-between",
//             alignItems: "center",
//             backgroundColor: "#f5f1ff",
//             padding: "8px 12px",
//             borderTop: "1px solid #e0e0e0",
//           }}
//         >
//           <Box>
//             <IconButton size="small" color="primary" onClick={() => handleFormat("bold")}>
//               <FormatBoldIcon />
//             </IconButton>
//             <IconButton size="small" color="primary" onClick={() => handleFormat("italic")}>
//               <FormatItalicIcon />
//             </IconButton>
//             <IconButton size="small" color="primary" onClick={() => handleFormat("underline")}>
//               <FormatUnderlinedIcon />
//             </IconButton>
//             <IconButton size="small" color="primary" onClick={() => handleFormat("list", "bullet")}>
//               <FormatListBulletedIcon />
//             </IconButton>
//           </Box>

//           <Box display="flex" alignItems="center" gap={1}>
//             <IconButton size="small" onClick={handleCopy} color="primary">
//               <ContentCopyIcon />
//             </IconButton>
//           </Box>
//         </Box>
//       </Box>

//       {/* Snackbar */}
//       <Snackbar
//         open={showToast}
//         autoHideDuration={2000}
//         onClose={() => setShowToast(false)}
//         anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
//       >
//         <Alert onClose={() => setShowToast(false)} severity="success" sx={{ width: "100%" }}>
//           Text copied!
//         </Alert>
//       </Snackbar>
//     </Box>
//   );
// };

// export default EditorBox;

import React, { useState } from "react";
import {
  Box,
  IconButton,
  Snackbar,
  Alert,
} from "@mui/material";
import FormatBoldIcon from "@mui/icons-material/FormatBold";
import FormatItalicIcon from "@mui/icons-material/FormatItalic";
import FormatUnderlinedIcon from "@mui/icons-material/FormatUnderlined";
import FormatListBulletedIcon from "@mui/icons-material/FormatListBulleted";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";

const EditorBox = ({ value, onChange }) => {
  const [showToast, setShowToast] = useState(false);

  // 🔹 Copy handler
  const handleCopy = () => {
    navigator.clipboard.writeText(value);
    setShowToast(true);
  };

  // 🔹 Text formatting handlers (basic JS logic)
  const handleFormat = (formatType) => {
    let formatted = value;
    switch (formatType) {
      case "bold":
        formatted = `**${value}**`;
        break;
      case "italic":
        formatted = `*${value}*`;
        break;
      case "underline":
        formatted = `__${value}__`;
        break;
      case "list":
        formatted = value
          .split("\n")
          .map((line) => (line.trim() ? `• ${line}` : ""))
          .join("\n");
        break;
      default:
        break;
    }
    onChange(formatted);
  };

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Outer Box */}
      <Box
        sx={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          border: "1px solid #e0e0e0",
          borderRadius: "8px",
          backgroundColor: "#fafafa",
          overflow: "hidden",
        }}
      >
        {/* Textarea */}
        <Box
          sx={{
            flex: 1,
            px: 2,
            pt: 2,
            "& textarea": {
              width: "100%",
              height: "100%",
              border: "none",
              outline: "none",
              resize: "none",
              backgroundColor: "#fafafa",
              fontSize: "14px",
              fontFamily: "inherit",
              lineHeight: 1.6,
              color: "#333",
            },
          }}
        >
          <textarea
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Write something..."
          />
        </Box>

        {/* Toolbar */}
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            backgroundColor: "#f5f1ff",
            padding: "8px 12px",
            borderTop: "1px solid #e0e0e0",
          }}
        >
          <Box>
            <IconButton
              size="small"
              color="primary"
              onClick={() => handleFormat("bold")}
            >
              <FormatBoldIcon />
            </IconButton>
            <IconButton
              size="small"
              color="primary"
              onClick={() => handleFormat("italic")}
            >
              <FormatItalicIcon />
            </IconButton>
            <IconButton
              size="small"
              color="primary"
              onClick={() => handleFormat("underline")}
            >
              <FormatUnderlinedIcon />
            </IconButton>
            <IconButton
              size="small"
              color="primary"
              onClick={() => handleFormat("list")}
            >
              <FormatListBulletedIcon />
            </IconButton>
          </Box>

          <Box display="flex" alignItems="center" gap={1}>
            <IconButton size="small" onClick={handleCopy} color="primary">
              <ContentCopyIcon />
            </IconButton>
          </Box>
        </Box>
      </Box>

      {/* Snackbar */}
      <Snackbar
        open={showToast}
        autoHideDuration={2000}
        onClose={() => setShowToast(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          onClose={() => setShowToast(false)}
          severity="success"
          sx={{ width: "100%" }}
        >
          Text copied!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default EditorBox;

