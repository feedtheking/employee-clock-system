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

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (editId) {
      await updateDoc(doc(db, "employees", editId), form);
      setEditId(null);
    } else {
      await addDoc(collection(db, "employees"), form);
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
  };

  const handleEdit = (emp) => {
    setForm(emp);
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
    }
  };


  
  // Apply search filters
  const filteredEmployees = employees.filter((emp) => {
    return (
      (search.employeeID === "" ||
        emp.employeeID
          ?.toLowerCase()
          .includes(search.employeeID.toLowerCase())) &&
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
          placeholder="PIN"
          value={form.pin}
          onChange={(e) => setForm({ ...form, pin: e.target.value })}
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

      {/* Search Filters */}
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

      {/* Employee Table */}
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
