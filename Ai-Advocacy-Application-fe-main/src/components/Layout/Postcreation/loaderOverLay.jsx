import React from "react";
import "./loader.css"; // 👈 CSS ko import karo

const LoaderOverlay = ({ loading,message }) => {
  if (!loading) return null;

  return (
    <div style={styles.overlay}>
      <div className="loader"></div>
      <p style={styles.text}> {message ? message : "Loading..."}</p>
    </div>
  );
};

const styles = {
  overlay: {
    position: "fixed",
    top: 0,
    left: 0,
    width: "100vw",
    height: "100vh",
    background: "rgba(255, 255, 255, 0.4)",
    backdropFilter: "blur(6px)", // ✅ background blur
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 2000,
  },
  text: {
    marginTop: "15px",
    fontSize: "18px",
    fontWeight: "600",
    color: "#333",
  },
};

export default LoaderOverlay;
