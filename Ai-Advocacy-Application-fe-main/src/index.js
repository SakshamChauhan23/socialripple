import React from "react";
import { createRoot } from "react-dom/client";
import Root from "./Root";
import "react-toastify/dist/ReactToastify.css";
import { ToastContainer } from "react-toastify";

const root = createRoot(document.getElementById("root"));
root.render(
  <>
    <ToastContainer
      position="top-right"
      autoClose={3000}
      hideProgressBar={false}
      newestOnTop={false}
      closeOnClick
      pauseOnFocusLoss
      draggable
      pauseOnHover
      theme="light"
      // #60f76de8
    />
    <Root />
  </>
);
