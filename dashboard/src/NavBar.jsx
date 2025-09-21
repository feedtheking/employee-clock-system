// src/NavBar.jsx
import React from "react";
import { Link } from "react-router-dom";
import { getAuth, signOut } from "firebase/auth";

const NavBar = ({ onLogout }) => {
  const navStyle = {
    padding: "10px",
    background: "#eee",
    marginBottom: "20px",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  };

  const linkStyle = {
    marginRight: "15px",
    textDecoration: "none",
    color: "blue",
  };

  const handleLogout = async () => {
    const auth = getAuth();
    await signOut(auth);
    if (onLogout) onLogout();
  };

  return (
    <div style={navStyle}>
      <div>
        <Link to="/" style={linkStyle}>Home</Link>
        <Link to="/employees" style={linkStyle}>Employees</Link>
        <Link to="/logs" style={linkStyle}>Logs</Link>
      </div>
      <button onClick={handleLogout}>Logout</button>
    </div>
  );
};

export default NavBar;
