const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */

// ...existing code...

const rateLimit = require("express-rate-limit");

// Rate limiting: max 5 uploads per minute per IP
const uploadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  message: "Too many uploads from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Multer storage config: only allow .pdf, sanitize filename
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, path.join(__dirname, "uploads"));
  },
  filename: function (req, file, cb) {
    // Only allow alphanumeric, dash, underscore, and .pdf extension
    const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9-_\.]/g, "");
    const ext = path.extname(safeName).toLowerCase();
    if (ext !== ".pdf") {
      return cb(new Error("Only PDF files are allowed"));
    }
    // Prevent overwriting: add timestamp
    const base = path.basename(safeName, ext);
    cb(null, `${base}-${Date.now()}${ext}`);
  },
});

// File filter: only accept PDF MIME type
const upload = multer({
  storage: storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max
  fileFilter: function (req, file, cb) {
    if (
      file.mimetype === "application/pdf" &&
      path.extname(file.originalname).toLowerCase() === ".pdf"
    ) {
      cb(null, true);
    } else {
      cb(new Error("Only PDF files are allowed"));
    }
  },
});

// ...existing code...

app.post(
  "/pdf/upload",
  uploadLimiter,
  upload.single("pdf"),
  (req, res) => {
    if (!req.file) {
      return res.status(400).json({ error: "No file uploaded or invalid file type." });
    }
    // No user input reflected, no XSS
    res.status(200).json({ message: "PDF uploaded successfully." });
  }
);

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
