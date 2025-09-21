// src/LogsPage.jsx
import React, { useState, useEffect } from "react";
import { collection, query, where, getDocs } from "firebase/firestore";
import { db } from "./firebase";

const LogsPage = () => {
  const [store, setStore] = useState("");
  const [stores, setStores] = useState([]);
  const [year, setYear] = useState(new Date().getFullYear());
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [includePhotos, setIncludePhotos] = useState(false);
  const [logs, setLogs] = useState([]);
  const [employees, setEmployees] = useState([]);

  // Month options
  const months = [
    { value: 1, label: "January" },
    { value: 2, label: "February" },
    { value: 3, label: "March" },
    { value: 4, label: "April" },
    { value: 5, label: "May" },
    { value: 6, label: "June" },
    { value: 7, label: "July" },
    { value: 8, label: "August" },
    { value: 9, label: "September" },
    { value: 10, label: "October" },
    { value: 11, label: "November" },
    { value: 12, label: "December" },
  ];

  // 🔹 Load store list for dropdown
  useEffect(() => {
    const fetchStores = async () => {
      const empRef = collection(db, "employees");
      const empSnap = await getDocs(empRef);
      const storeSet = new Set();
      empSnap.docs.forEach((doc) => {
        const data = doc.data();
        if (data.store) {
          storeSet.add(data.store);
        }
      });
      setStores([...storeSet]);
    };
    fetchStores();
  }, []);

  // 🔹 Fetch logs
  const fetchLogs = async () => {
    try {
      // Employees in this store
      const empRef = collection(db, "employees");
      const empQ = query(empRef, where("store", "==", store), where("employedStatus", "==", true));
      const empSnap = await getDocs(empQ);

      const employeeList = empSnap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));
      setEmployees(employeeList);
      console.log("Employees:", employeeList);

      if (employeeList.length === 0) {
        setLogs([]);
        return;
      }

      // Date range
      const start = new Date(year, month - 1, 1);
      const end = new Date(year, month, 1);

      // Logs in this month
      const logRef = collection(db, "logs");
      const logQ = query(
        logRef,
        where("timestamp", ">=", start),
        where("timestamp", "<", end)
      );
      const logSnap = await getDocs(logQ);

      const logList = logSnap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      console.log("Raw logs from Firestore:", logList);

      // Match logs to employees by employeeID
      const filteredLogs = logList.filter((log) =>
        employeeList.some((emp) => emp.employeeID === log.employeeID)
      );

      console.log("Filtered logs:", filteredLogs);

      setLogs(filteredLogs);
    } catch (err) {
      console.error("Error fetching logs:", err);
    }
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>Employee Logs</h2>
      <div style={{ marginBottom: "20px" }}>
        <select value={store} onChange={(e) => setStore(e.target.value)}>
          <option value="">Select Store</option>
          {stores.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select value={month} onChange={(e) => setMonth(Number(e.target.value))}>
          {months.map((m) => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </select>
        <input
          type="number"
          placeholder="Year"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
        />
        <label>
          <input
            type="checkbox"
            checked={includePhotos}
            onChange={(e) => setIncludePhotos(e.target.checked)}
          />
          Include Photos
        </label>
        <button onClick={fetchLogs}>Fetch Logs</button>
      </div>

      {employees.map((emp) => (
        <div key={emp.id} style={{ marginBottom: "20px" }}>
          <h3>
            {emp.lastName}, {emp.firstName} ({emp.store})
          </h3>
          <ul>
            {logs
              .filter((log) => log.employeeID === emp.employeeID)
              .map((log) => (
                <li key={log.id}>
                  {log.action} -{" "}
                  {log.timestamp?.toDate
                    ? log.timestamp.toDate().toISOString().replace("T", " ").substring(0, 19)
                    : ""}
                  {includePhotos && log.photoURL && (
                    <img
                      src={log.photoURL}
                      alt="proof"
                      style={{ width: "50px", marginLeft: "10px" }}
                    />
                  )}
                </li>
              ))}
          </ul>
        </div>
      ))}
    </div>
  );
};

export default LogsPage;
