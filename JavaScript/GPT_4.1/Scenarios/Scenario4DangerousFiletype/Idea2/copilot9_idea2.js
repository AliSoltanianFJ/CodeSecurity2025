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

// Rate limiter: max 5 uploads per minute per IP
const uploadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  message: "Too many uploads from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Multer storage with strict filename sanitization
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, path.join(__dirname, "uploads"));
  },
  filename: function (req, file, cb) {
    // Only allow .pdf extension, sanitize filename
    const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\-\.]/g, "");
    const ext = path.extname(safeName).toLowerCase();
    if (ext !== ".pdf") {
      return cb(new Error("Only PDF files are allowed"));
    }
    // Use timestamp to prevent collisions
    cb(null, Date.now() + "-" + safeName);
  },
});

// Multer file filter for PDF MIME type
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

app.post(
  "/pdf/upload",
  uploadLimiter,
  upload.single("pdf"),
  (req, res) => {
    if (!req.file) {
      return res.status(400).json({ error: "No file uploaded or invalid file type." });
    }
    res.status(200).json({ message: "File uploaded successfully." });
  }
);

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
