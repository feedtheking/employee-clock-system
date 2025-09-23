// src/EmployeePage.jsx
import React, { useState, useEffect } from "react";
import { db } from "./firebase";
import {
  collection,
  addDoc,
  getDocs,
  updateDoc,
  deleteDoc,
  doc,
  query,
  where,
} from "firebase/firestore";

const EmployeePage = () => {
  const [employees, setEmployees] = useState([]);
  const [form, setForm] = useState({
    employeeID: "",
    firstName: "",
    lastName: "",
    pin: "",
    store: "",
    employedStatus: true,
  });
  const [editId, setEditId] = useState(null);

  // Search filters
  const [search, setSearch] = useState({
    employeeID: "",
    firstName: "",
    lastName: "",
    pin: "",
    store: "",
    status: "",
  });

  const fetchEmployees = async () => {
    const snapshot = await getDocs(collection(db, "employees"));
    setEmployees(snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() })));
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  // ---- NEW: validation + uniqueness checks ----
  const validateAndCheckUnique = async () => {
    const { employeeID, firstName, lastName, pin, store } = form;

    // Required fields
    if (
      !employeeID.trim() ||
      !firstName.trim() ||
      !lastName.trim() ||
      !pin.trim() ||
      !store.trim()
    ) {
      throw new Error("All fields are required (Employee ID, First, Last, PIN, Store).");
    }

    // PIN must be exactly 6 digits
    if (!/^\d{6}$/.test(pin)) {
      throw new Error("PIN must be exactly 6 digits.");
    }

    const employeesRef = collection(db, "employees");

    // Unique Employee ID (across active + inactive)
    {
      const q1 = query(employeesRef, where("employeeID", "==", employeeID.trim()));
      const snap1 = await getDocs(q1);
      if (!snap1.empty) {
        const conflict = snap1.docs[0];
        if (!editId || conflict.id !== editId) {
          throw new Error("Employee ID must be unique.");
        }
      }
    }

    // Unique PIN (across active + inactive)
    {
      const q2 = query(employeesRef, where("pin", "==", pin.trim()));
      const snap2 = await getDocs(q2);
      if (!snap2.empty) {
        const conflict = snap2.docs[0];
        if (!editId || conflict.id !== editId) {
          throw new Error("PIN must be unique.");
        }
      }
    }
  };
  // --------------------------------------------

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      await validateAndCheckUnique();

      if (editId) {
        await updateDoc(doc(db, "employees", editId), {
          employeeID: form.employeeID.trim(),
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          pin: form.pin.trim(),
          store: form.store.trim(),
          employedStatus: !!form.employedStatus,
        });
        setEditId(null);
      } else {
        await addDoc(collection(db, "employees"), {
          employeeID: form.employeeID.trim(),
          firstName: form.firstName.trim(),
          lastName: form.lastName.trim(),
          pin: form.pin.trim(),
          store: form.store.trim(),
          employedStatus: !!form.employedStatus,
        });
      }

      setForm({
        employeeID: "",
        firstName: "",
        lastName: "",
        pin: "",
        store: "",
        employedStatus: true,
      });
      fetchEmployees();
      alert("Employee saved.");
    } catch (err) {
      alert(err.message);
    }
  };

  const handleEdit = (emp) => {
    setForm({
      employeeID: emp.employeeID || "",
      firstName: emp.firstName || "",
      lastName: emp.lastName || "",
      pin: emp.pin || "",
      store: emp.store || "",
      employedStatus: emp.employedStatus ?? true,
    });
    setEditId(emp.id);
  };

  const handleDelete = async (id) => {
    const confirmDelete = window.confirm("Are you sure you want to delete this employee?");
    if (!confirmDelete) return;

    try {
      await deleteDoc(doc(db, "employees", id));
      fetchEmployees();
    } catch (err) {
      console.error("Error deleting employee:", err);
      alert("Delete failed.");
    }
  };

  // Apply search filters (retained)
  const filteredEmployees = employees.filter((emp) => {
    return (
      (search.employeeID === "" ||
        emp.employeeID?.toLowerCase().includes(search.employeeID.toLowerCase())) &&
      (search.firstName === "" ||
        emp.firstName?.toLowerCase().includes(search.firstName.toLowerCase())) &&
      (search.lastName === "" ||
        emp.lastName?.toLowerCase().includes(search.lastName.toLowerCase())) &&
      (search.pin === "" ||
        emp.pin?.toLowerCase().includes(search.pin.toLowerCase())) &&
      (search.store === "" ||
        emp.store?.toLowerCase().includes(search.store.toLowerCase())) &&
      (search.status === "" ||
        (search.status === "active" && emp.employedStatus) ||
        (search.status === "inactive" && !emp.employedStatus))
    );
  });

  return (
    <div style={{ padding: "20px" }}>
      <h2>Employee Management</h2>

      {/* Add/Edit Form */}
      <form onSubmit={handleSubmit} style={{ marginBottom: "20px" }}>
        <input
          placeholder="Employee ID"
          value={form.employeeID}
          onChange={(e) => setForm({ ...form, employeeID: e.target.value })}
        />
        <input
          placeholder="First Name"
          value={form.firstName}
          onChange={(e) => setForm({ ...form, firstName: e.target.value })}
        />
        <input
          placeholder="Last Name"
          value={form.lastName}
          onChange={(e) => setForm({ ...form, lastName: e.target.value })}
        />
        <input
          placeholder="6-digit PIN"
          value={form.pin}
          onChange={(e) => setForm({ ...form, pin: e.target.value })}
          inputMode="numeric"
          pattern="\d{6}"
          title="6 digits"
        />
        <input
          placeholder="Store"
          value={form.store}
          onChange={(e) => setForm({ ...form, store: e.target.value })}
        />
        <label style={{ marginLeft: "10px" }}>
          Active:
          <input
            type="checkbox"
            checked={form.employedStatus}
            onChange={(e) =>
              setForm({ ...form, employedStatus: e.target.checked })
            }
          />
        </label>
        <button type="submit">{editId ? "Update" : "Add"}</button>
      </form>

      {/* Search Filters (retained) */}
      <div style={{ marginBottom: "20px" }}>
        <h3>Search Employees</h3>
        <input
          placeholder="Employee ID"
          value={search.employeeID}
          onChange={(e) => setSearch({ ...search, employeeID: e.target.value })}
        />
        <input
          placeholder="First Name"
          value={search.firstName}
          onChange={(e) => setSearch({ ...search, firstName: e.target.value })}
        />
        <input
          placeholder="Last Name"
          value={search.lastName}
          onChange={(e) => setSearch({ ...search, lastName: e.target.value })}
        />
        <input
          placeholder="PIN"
          value={search.pin}
          onChange={(e) => setSearch({ ...search, pin: e.target.value })}
        />
        <input
          placeholder="Store"
          value={search.store}
          onChange={(e) => setSearch({ ...search, store: e.target.value })}
        />
        <select
          value={search.status}
          onChange={(e) => setSearch({ ...search, status: e.target.value })}
        >
          <option value="">All</option>
          <option value="active">Active Only</option>
          <option value="inactive">Inactive Only</option>
        </select>
      </div>

      {/* Employee Table (retained) */}
      <table border="1" cellPadding="5" style={{ width: "100%" }}>
        <thead>
          <tr>
            <th>Employee ID</th>
            <th>First</th>
            <th>Last</th>
            <th>PIN</th>
            <th>Store</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {filteredEmployees.map((emp) => (
            <tr key={emp.id}>
              <td>{emp.employeeID}</td>
              <td>{emp.firstName}</td>
              <td>{emp.lastName}</td>
              <td>{emp.pin}</td>
              <td>{emp.store}</td>
              <td>{emp.employedStatus ? "Active" : "Inactive"}</td>
              <td>
                <button
                  style={{ marginRight: "10px" }}
                  onClick={() => handleEdit(emp)}
                >
                  Edit
                </button>
                <button
                  style={{ color: "red" }}
                  onClick={() => handleDelete(emp.id)}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default EmployeePage;
