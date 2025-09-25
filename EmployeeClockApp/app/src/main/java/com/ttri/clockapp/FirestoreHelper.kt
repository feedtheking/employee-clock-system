package com.ttri.clockapp

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    // 🔹 Add new employee
    suspend fun addEmployee(employee: Employee): Boolean {
        return try {
            db.collection("employees")
                .document(employee.pin) // use PIN as unique ID
                .set(employee)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 🔹 Get all employees
    suspend fun getEmployees(): List<Employee> {
        return try {
            val snapshot = db.collection("employees").get().await()
            snapshot.toObjects(Employee::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 🔹 Get single employee by PIN
    suspend fun getEmployeeByPin(pin: String): Employee? {
        return try {
            val doc = db.collection("employees")
                .document(pin)
                .get()
                .await()
            if (doc.exists()) doc.toObject(Employee::class.java) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 🔹 Delete employee
    suspend fun deleteEmployee(pin: String): Boolean {
        return try {
            db.collection("employees")
                .document(pin)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
