package com.ttri.clockapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Task

object FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    // 🔹 Add new employee
    fun addEmployee(employee: Employee, onResult: (Boolean) -> Unit) {
        db.collection("employees")
            .document(employee.pin) // use PIN as unique ID
            .set(employee)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // 🔹 Get all employees
    fun getEmployees(onResult: (List<Employee>) -> Unit) {
        db.collection("employees")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val employees = result.toObjects(Employee::class.java)
                onResult(employees)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 🔹 Get single employee by PIN
    fun getEmployeeByPin(pin: String, onResult: (Employee?) -> Unit) {
        db.collection("employees")
            .document(pin)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(doc.toObject(Employee::class.java))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // 🔹 Delete employee
    fun deleteEmployee(pin: String, onResult: (Boolean) -> Unit) {
        db.collection("employees")
            .document(pin)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
