import React, { useEffect, useState } from "react";
import {
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDocs,
  query,
  where,
  updateDoc,
  Timestamp,
} from "firebase/firestore";
import { getStorage, ref, deleteObject } from "firebase/storage";
import { db } from "./firebase";

const storage = getStorage();

const photoAgeOptionsStore = [
  { value: "30", label: "Photos older than 30 days" },
  { value: "60", label: "Photos older than 60 days" },
  { value: "90", label: "Photos older than 90 days" },
  { value: "all", label: "All photos" },
];

const photoAgeOptionsEmployee = [
  { value: "30", label: "Photos older than 30 days" },
  { value: "60", label: "Photos older than 60 days" },
  { value: "90", label: "Photos older than 90 days" },
  { value: "all", label: "All photos" },
];

const logAgeOptions = [
  { value: "30", label: "Logs older than 30 days" },
  { value: "60", label: "Logs older than 60 days" },
  { value: "90", label: "Logs older than 90 days" },
  { value: "all", label: "All logs" },
];

const getOptionLabel = (options, value) => {
  const match = options.find((option) => option.value === value);
  return match ? match.label : value;
};

const computeCutoff = (value) => {
  if (value === "all") {
    return null;
  }

  const days = Number(value);
  if (Number.isNaN(days)) {
    return null;
  }

  return new Date(Date.now() - days * 24 * 60 * 60 * 1000);
};

const tryDeleteFromStorage = async (path) => {
  if (!path) {
    return { success: false };
  }

  try {
    const storageRef = ref(storage, path);
    await deleteObject(storageRef);
    return { success: true };
  } catch (error) {
    if (error?.code === "storage/object-not-found") {
      return { success: true };
    }
    console.error("Failed to delete storage object", path, error);
    return { success: false, error };
  }
};

const CleanupPage = () => {
  const [stores, setStores] = useState([]);

  const [storeFilter, setStoreFilter] = useState("");
  const [storeAgeFilter, setStoreAgeFilter] = useState(photoAgeOptionsStore[0].value);

  const [employeeIdFilter, setEmployeeIdFilter] = useState("");
  const [employeeAgeFilter, setEmployeeAgeFilter] = useState(photoAgeOptionsEmployee[0].value);

  const [logStoreFilter, setLogStoreFilter] = useState("");
  const [logStoreAgeFilter, setLogStoreAgeFilter] = useState(logAgeOptions[0].value);

  const [logEmployeeFilter, setLogEmployeeFilter] = useState("");
  const [logEmployeeAgeFilter, setLogEmployeeAgeFilter] = useState(logAgeOptions[0].value);

  const [statusMessage, setStatusMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const fetchStores = async () => {
      try {
        const snapshot = await getDocs(collection(db, "employees"));
        const uniqueStores = Array.from(
          new Set(snapshot.docs.map((docSnap) => docSnap.data().store))
        )
          .filter(Boolean)
          .sort((a, b) => a.localeCompare(b));
        setStores(uniqueStores);
      } catch (err) {
        console.error("Failed to load stores", err);
        setErrorMessage("Unable to load stores. Please refresh and try again.");
      }
    };

    fetchStores();
  }, []);

  const loadEmployeesForStore = async (storeValue) => {
    const trimmedStore = storeValue.trim();

    if (!trimmedStore) {
      return null;
    }

    const employeesQuery = query(
      collection(db, "employees"),
      where("store", "==", trimmedStore)
    );
    const employeesSnap = await getDocs(employeesQuery);
    return employeesSnap.docs.map((docSnap) => ({
      id: docSnap.id,
      ...docSnap.data(),
    }));
  };

  const confirmAction = (title, scopeDescription) => {
    return window.confirm(`${title}\n\n${scopeDescription}`);
  };

  const resetMessages = () => {
    setStatusMessage("");
    setErrorMessage("");
  };

  const cleanupPhotoLogs = async ({ logs, ageLabel, origin }) => {
    let photosRemoved = 0;
    let failedPaths = 0;
    const affectedEmployees = new Set();

    for (const log of logs) {
      const updates = {};
      let removedForLog = 0;

      if (log.photoInPath) {
        const result = await tryDeleteFromStorage(log.photoInPath);
        if (result.success) {
          removedForLog += 1;
          updates.photoInPath = deleteField();
          if (log.photoIn) {
            updates.photoIn = deleteField();
          }
        } else {
          failedPaths += 1;
        }
      }

      if (log.photoOutPath) {
        const result = await tryDeleteFromStorage(log.photoOutPath);
        if (result.success) {
          removedForLog += 1;
          updates.photoOutPath = deleteField();
          if (log.photoOut) {
            updates.photoOut = deleteField();
          }
        } else {
          failedPaths += 1;
        }
      }

      if (log.photoURL && removedForLog > 0) {
        updates.photoURL = deleteField();
      }

      if (removedForLog > 0 && Object.keys(updates).length > 0) {
        await updateDoc(doc(db, "logs", log.id), updates);
        photosRemoved += removedForLog;
        affectedEmployees.add(log.employeeID || "unknown");
      }
    }

    if (photosRemoved === 0) {
      setStatusMessage(`No photos were removed for ${origin}. Nothing matched the filters.`);
      return { photosRemoved, failedPaths };
    }

    setStatusMessage(
      `Deleted ${photosRemoved} photos across ${affectedEmployees.size} employees (${origin}, ${ageLabel}).`
    );

    if (failedPaths > 0) {
      setErrorMessage(
        `${failedPaths} photos could not be removed. Check console for details.`
      );
    }

    return { photosRemoved, failedPaths };
  };

  const deleteLogsAndPhotos = async ({ logs, ageLabel, origin }) => {
    let photosRemoved = 0;
    let logsRemoved = 0;
    let failedPaths = 0;
    const affectedEmployees = new Set();

    for (const log of logs) {
      let removedForLog = 0;

      if (log.photoInPath) {
        const result = await tryDeleteFromStorage(log.photoInPath);
        if (result.success) {
          removedForLog += 1;
        } else {
          failedPaths += 1;
        }
      }

      if (log.photoOutPath) {
        const result = await tryDeleteFromStorage(log.photoOutPath);
        if (result.success) {
          removedForLog += 1;
        } else {
          failedPaths += 1;
        }
      }

      if (removedForLog > 0) {
        photosRemoved += removedForLog;
      }

      await deleteDoc(doc(db, "logs", log.id));
      logsRemoved += 1;
      affectedEmployees.add(log.employeeID || "unknown");
    }

    setStatusMessage(
      `Deleted ${logsRemoved} logs and ${photosRemoved} photos (${origin}, ${ageLabel}).`
    );

    if (failedPaths > 0) {
      setErrorMessage(
        `${failedPaths} photos could not be removed. Check console for details.`
      );
    }
  };

  const handlePhotoDeleteByStore = async () => {
    resetMessages();

    const ageLabel = getOptionLabel(photoAgeOptionsStore, storeAgeFilter);
    const scopeDescription = `Store: ${storeFilter || "All stores"}` +
      `\nAge filter: ${ageLabel}`;

    if (!confirmAction("Delete photos by store?", scopeDescription)) {
      return;
    }

    setIsProcessing(true);

    try {
      const cutoffDate = computeCutoff(storeAgeFilter);
      const constraints = [];
      if (cutoffDate) {
        constraints.push(where("timestamp", "<=", Timestamp.fromDate(cutoffDate)));
      }

      const logsRef = collection(db, "logs");
      const logsQuery = constraints.length ? query(logsRef, ...constraints) : logsRef;
      const logsSnap = await getDocs(logsQuery);
      let logs = logsSnap.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));

      if (storeFilter.trim()) {
        const employees = await loadEmployeesForStore(storeFilter);
        const allowedEmployeeIds = new Set(
          (employees || []).map((emp) => emp.employeeID)
        );

        logs = logs.filter((log) => allowedEmployeeIds.has(log.employeeID));
      }

      logs = logs.filter((log) => log.photoInPath || log.photoOutPath);

      if (logs.length === 0) {
        setStatusMessage("No photos matched the selected filters.");
        return;
      }

      await cleanupPhotoLogs({
        logs,
        ageLabel,
        origin: "photo delete by store",
      });
    } catch (error) {
      console.error("Photo deletion by store failed", error);
      setErrorMessage("Photo deletion failed. Please try again.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handlePhotoDeleteByEmployee = async () => {
    resetMessages();

    const employeeId = employeeIdFilter.trim();
    if (!employeeId) {
      setErrorMessage("Employee ID is required for photo delete via employee.");
      return;
    }

    const ageLabel = getOptionLabel(photoAgeOptionsEmployee, employeeAgeFilter);
    const scopeDescription = `Employee ID: ${employeeId}` +
      `\nAge filter: ${ageLabel}`;

    if (!confirmAction("Delete photos by employee?", scopeDescription)) {
      return;
    }

    setIsProcessing(true);

    try {
      const cutoffDate = computeCutoff(employeeAgeFilter);
      const constraints = [where("employeeID", "==", employeeId)];
      if (cutoffDate) {
        constraints.push(where("timestamp", "<=", Timestamp.fromDate(cutoffDate)));
      }

      const logsQuery = query(collection(db, "logs"), ...constraints);
      const logsSnap = await getDocs(logsQuery);
      const logs = logsSnap.docs
        .map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }))
        .filter((log) => log.photoInPath || log.photoOutPath);

      if (logs.length === 0) {
        setStatusMessage("No photos matched the selected filters.");
        return;
      }

      await cleanupPhotoLogs({
        logs,
        ageLabel,
        origin: "photo delete by employee",
      });
    } catch (error) {
      console.error("Photo deletion by employee failed", error);
      setErrorMessage("Photo deletion failed. Please try again.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleLogsDeleteByStore = async () => {
    resetMessages();

    const ageLabel = getOptionLabel(logAgeOptions, logStoreAgeFilter);
    const scopeDescription = `Store: ${logStoreFilter || "All stores"}` +
      `\nAge filter: ${ageLabel}`;

    if (!confirmAction("Delete logs and photos by store?", scopeDescription)) {
      return;
    }

    setIsProcessing(true);

    try {
      const cutoffDate = computeCutoff(logStoreAgeFilter);
      const constraints = [];
      if (cutoffDate) {
        constraints.push(where("timestamp", "<=", Timestamp.fromDate(cutoffDate)));
      }

      const logsRef = collection(db, "logs");
      const logsQuery = constraints.length ? query(logsRef, ...constraints) : logsRef;
      const logsSnap = await getDocs(logsQuery);
      let logs = logsSnap.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));

      if (logStoreFilter.trim()) {
        const employees = await loadEmployeesForStore(logStoreFilter);
        const allowedEmployeeIds = new Set(
          (employees || []).map((emp) => emp.employeeID)
        );

        logs = logs.filter((log) => allowedEmployeeIds.has(log.employeeID));
      }

      if (logs.length === 0) {
        setStatusMessage("No logs matched the selected filters.");
        return;
      }

      await deleteLogsAndPhotos({
        logs,
        ageLabel,
        origin: "logs delete by store",
      });
    } catch (error) {
      console.error("Log deletion by store failed", error);
      setErrorMessage("Log deletion failed. Please try again.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleLogsDeleteByEmployee = async () => {
    resetMessages();

    const employeeId = logEmployeeFilter.trim();
    if (!employeeId) {
      setErrorMessage("Employee ID is required for logs delete via employee.");
      return;
    }

    const ageLabel = getOptionLabel(logAgeOptions, logEmployeeAgeFilter);
    const scopeDescription = `Employee ID: ${employeeId}` +
      `\nAge filter: ${ageLabel}`;

    if (!confirmAction("Delete logs and photos by employee?", scopeDescription)) {
      return;
    }

    setIsProcessing(true);

    try {
      const cutoffDate = computeCutoff(logEmployeeAgeFilter);
      const constraints = [where("employeeID", "==", employeeId)];
      if (cutoffDate) {
        constraints.push(where("timestamp", "<=", Timestamp.fromDate(cutoffDate)));
      }

      const logsQuery = query(collection(db, "logs"), ...constraints);
      const logsSnap = await getDocs(logsQuery);
      const logs = logsSnap.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));

      if (logs.length === 0) {
        setStatusMessage("No logs matched the selected filters.");
        return;
      }

      await deleteLogsAndPhotos({
        logs,
        ageLabel,
        origin: "logs delete by employee",
      });
    } catch (error) {
      console.error("Log deletion by employee failed", error);
      setErrorMessage("Log deletion failed. Please try again.");
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>Cleanup Tools</h2>
      <p>Remove photos and logs from Firebase Storage and Firestore using the criteria below.</p>

      {statusMessage && (
        <div style={{ margin: "10px 0", color: "green" }}>{statusMessage}</div>
      )}
      {errorMessage && (
        <div style={{ margin: "10px 0", color: "red" }}>{errorMessage}</div>
      )}

      <section style={{ marginBottom: "40px", paddingBottom: "20px", borderBottom: "1px solid #ccc" }}>
        <h3>Photo Cleanup</h3>

        <div style={{ marginBottom: "25px" }}>
          <h4>Photo Delete via Store</h4>
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", flexWrap: "wrap" }}>
            <select
              value={storeFilter}
              onChange={(e) => setStoreFilter(e.target.value)}
            >
              <option value="">All Stores</option>
              {stores.map((store) => (
                <option key={store} value={store}>
                  {store}
                </option>
              ))}
            </select>
            <select
              value={storeAgeFilter}
              onChange={(e) => setStoreAgeFilter(e.target.value)}
            >
              {photoAgeOptionsStore.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button onClick={handlePhotoDeleteByStore} disabled={isProcessing}>
              {isProcessing ? "Processing..." : "Delete Photos"}
            </button>
          </div>
          <small>
            Leave store blank to process every store. Only photos associated with matched logs are removed.
          </small>
        </div>

        <div>
          <h4>Photo Delete via Employee</h4>
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", flexWrap: "wrap" }}>
            <input
              placeholder="Employee ID"
              value={employeeIdFilter}
              onChange={(e) => setEmployeeIdFilter(e.target.value)}
            />
            <select
              value={employeeAgeFilter}
              onChange={(e) => setEmployeeAgeFilter(e.target.value)}
            >
              {photoAgeOptionsEmployee.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button onClick={handlePhotoDeleteByEmployee} disabled={isProcessing}>
              {isProcessing ? "Processing..." : "Delete Photos"}
            </button>
          </div>
          <small>Employee ID is required for this action.</small>
        </div>
      </section>

      <section>
        <h3>Logs + Photo Cleanup</h3>

        <div style={{ marginBottom: "25px" }}>
          <h4>Logs + Photo Delete via Store</h4>
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", flexWrap: "wrap" }}>
            <select
              value={logStoreFilter}
              onChange={(e) => setLogStoreFilter(e.target.value)}
            >
              <option value="">All Stores</option>
              {stores.map((store) => (
                <option key={store} value={store}>
                  {store}
                </option>
              ))}
            </select>
            <select
              value={logStoreAgeFilter}
              onChange={(e) => setLogStoreAgeFilter(e.target.value)}
            >
              {logAgeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button onClick={handleLogsDeleteByStore} disabled={isProcessing}>
              {isProcessing ? "Processing..." : "Delete Logs & Photos"}
            </button>
          </div>
          <small>Deletes matching logs and their photos. Leave store blank to cover every store.</small>
        </div>

        <div>
          <h4>Logs + Photo Delete via Employee</h4>
          <div style={{ display: "flex", gap: "10px", marginBottom: "10px", flexWrap: "wrap" }}>
            <input
              placeholder="Employee ID"
              value={logEmployeeFilter}
              onChange={(e) => setLogEmployeeFilter(e.target.value)}
            />
            <select
              value={logEmployeeAgeFilter}
              onChange={(e) => setLogEmployeeAgeFilter(e.target.value)}
            >
              {logAgeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <button onClick={handleLogsDeleteByEmployee} disabled={isProcessing}>
              {isProcessing ? "Processing..." : "Delete Logs & Photos"}
            </button>
          </div>
          <small>Employee ID is required. Deletes both logs and linked photos.</small>
        </div>
      </section>
    </div>
  );
};

export default CleanupPage;
