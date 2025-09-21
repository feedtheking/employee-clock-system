// src/LogsPage.jsx
import React, { useState, useEffect } from "react";
import {
  collection,
  query,
  where,
  getDocs,
} from "firebase/firestore";
import { db } from "./firebase";

const LogsPage = () => {
  const [store, setStore] = useState("");
  const [stores, setStores] = useState([]);
  const [year, setYear] = useState(new Date().getFullYear());
  const [month, setMonth] = useState(new Date().getMonth() + 1);
  const [includePhotos, setIncludePhotos] = useState(false);
  const [employees, setEmployees] = useState([]);
  const [logs, setLogs] = useState([]);

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

  // Fetch distinct stores for dropdown
  useEffect(() => {
    const fetchStores = async () => {
      const empSnap = await getDocs(collection(db, "employees"));
      const allStores = Array.from(
        new Set(empSnap.docs.map((doc) => doc.data().store))
      ).filter(Boolean);
      setStores(allStores);
    };
    fetchStores();
  }, []);

  const fetchLogs = async () => {
    try {
      if (!store) {
        alert("Please select a store first.");
        return;
      }

      // Get employees in selected store
      const empRef = collection(db, "employees");
      const empQ = query(
        empRef,
        where("store", "==", store),
        where("employedStatus", "==", true)
      );
      const empSnap = await getDocs(empQ);
      const employeeList = empSnap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      setEmployees(employeeList);

      if (employeeList.length === 0) {
        setLogs([]);
        return;
      }

      // Month range
      const start = new Date(year, month - 1, 1);
      const end = new Date(year, month, 1);

      // Fetch logs for this month
      const logRef = collection(db, "logs");
      const logQ = query(
        logRef,
        where("timestamp", ">=", start),
        where("timestamp", "<", end)
      );
      const logSnap = await getDocs(logQ);

      const allLogs = logSnap.docs.map((doc) => {
        const data = doc.data();
        return includePhotos
          ? { id: doc.id, ...data } // include photoURL
          : {
              id: doc.id,
              employeeID: data.employeeID,
              action: data.action,
              timestamp: data.timestamp,
              // photoURL omitted
            };
      });


      // Filter logs to employees of this store
      const filteredLogs = allLogs.filter((log) =>
        employeeList.some((emp) => emp.employeeID === log.employeeID)
      );

      setLogs(filteredLogs);
    } catch (err) {
      console.error("Error fetching logs:", err);
    }
  };

  const exportCSV = () => {
    if (logs.length === 0) return;

    let csv =
      "EmployeeID,First Name,Last Name,Store,Clock In,Clock Out";
    if (includePhotos) csv += ",Photo In,Photo Out";
    csv += "\n";

    // Sort employees by last, then first
    const sortedEmps = [...employees].sort((a, b) => {
      if (a.lastName !== b.lastName) {
        return a.lastName.localeCompare(b.lastName);
      }
      return a.firstName.localeCompare(b.firstName);
    });

    sortedEmps.forEach((emp) => {
      // Get logs for this employee, sort by timestamp
      const empLogs = logs
        .filter((l) => l.employeeID === emp.employeeID)
        .sort(
          (a, b) =>
            a.timestamp.toDate() - b.timestamp.toDate()
        );

      for (let i = 0; i < empLogs.length; i += 2) {
        const clockIn = empLogs[i];
        const clockOut = empLogs[i + 1];

        const inTime = clockIn?.timestamp
          ? clockIn.timestamp.toDate().toISOString().replace("T", " ").substring(0, 19)
          : "";
        const outTime = clockOut?.timestamp
          ? clockOut.timestamp.toDate().toISOString().replace("T", " ").substring(0, 19)
          : "";

        let row = `${emp.employeeID},${emp.firstName},${emp.lastName},${emp.store},${inTime},${outTime}`;
        if (includePhotos) {
          row += `,${clockIn?.photoURL || ""},${clockOut?.photoURL || ""}`;
        }
        csv += row + "\n";
      }
      csv += "\n";
    });

    const blob = new Blob([csv], { type: "text/csv" });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `logs_${store}_${year}_${month}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>Logs Viewer</h2>

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
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
        />

        <label style={{ marginLeft: "10px" }}>
          <input
            type="checkbox"
            checked={includePhotos}
            onChange={(e) => setIncludePhotos(e.target.checked)}
          />
          Include Photos
        </label>

        <button onClick={fetchLogs}>Fetch Logs</button>
        <button onClick={exportCSV}>Export CSV</button>
      </div>

      {/* Table view grouped by employee */}
      {employees.length > 0 &&
        [...employees]
          .sort((a, b) => {
            if (a.lastName !== b.lastName) {
              return a.lastName.localeCompare(b.lastName);
            }
            return a.firstName.localeCompare(b.firstName);
          })
          .map((emp) => {
            const empLogs = logs
              .filter((l) => l.employeeID === emp.employeeID)
              .sort((a, b) => a.timestamp.toDate() - b.timestamp.toDate());

            return (
              <div key={emp.employeeID} style={{ marginBottom: "30px" }}>
                <h3>
                  {emp.lastName}, {emp.firstName} ({emp.store})
                </h3>
                <table border="1" cellPadding="5" style={{ width: "100%" }}>
                  <thead>
                    <tr>
                      <th>Clock In</th>
                      <th>Clock Out</th>
                      {includePhotos && (
                        <>
                          <th>Photo In</th>
                          <th>Photo Out</th>
                        </>
                      )}
                    </tr>
                  </thead>
                  <tbody>
                    {empLogs.map((log, i) => {
                      if (i % 2 === 0) {
                        const clockIn = log;
                        const clockOut = empLogs[i + 1];
                        return (
                          <tr key={log.id}>
                            <td>
                              {clockIn.timestamp
                                ? clockIn.timestamp.toDate().toISOString().replace("T", " ").substring(0, 19)
                                : ""}
                            </td>
                            <td>
                              {clockOut?.timestamp
                                ? clockOut.timestamp.toDate().toISOString().replace("T", " ").substring(0, 19)
                                : ""}
                            </td>
                            {includePhotos && (
                              <>
                                <td>
                                  {clockIn.photoURL && (
                                    <img
                                      src={clockIn.photoURL}
                                      alt="in"
                                      style={{ width: "50px" }}
                                    />
                                  )}
                                </td>
                                <td>
                                  {clockOut?.photoURL && (
                                    <img
                                      src={clockOut.photoURL}
                                      alt="out"
                                      style={{ width: "50px" }}
                                    />
                                  )}
                                </td>
                              </>
                            )}
                          </tr>
                        );
                      }
                      return null;
                    })}
                  </tbody>
                </table>
              </div>
            );
          })}
    </div>
  );
};

export default LogsPage;
