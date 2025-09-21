// src/firebase.js
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";

// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyBdmkUd0lQsdWLjaO5OLAnm0by9qAFDR_w",
  authDomain: "employee-kiosk.firebaseapp.com",
  projectId: "employee-kiosk",
  storageBucket: "employee-kiosk.firebasestorage.app",
  messagingSenderId: "1014767073253",
  appId: "1:1014767073253:web:3932d8fa257e62e4d5210c",
  measurementId: "G-MZHRPPF85C"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
