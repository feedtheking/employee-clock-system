// src/main.jsx
import React, { useState } from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "./HomePage";
import EmployeePage from "./EmployeePage";
import LogsPage from "./LogsPage";
import LoginPage from "./LoginPage";
import NavBar from "./NavBar";

const App = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  return (
    <BrowserRouter>
      {isLoggedIn ? (
        <div>
          <NavBar onLogout={() => setIsLoggedIn(false)} />
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/employees" element={<EmployeePage />} />
            <Route path="/logs" element={<LogsPage />} />
          </Routes>
        </div>
      ) : (
        <LoginPage onLogin={() => setIsLoggedIn(true)} />
      )}
    </BrowserRouter>
  );
};

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
