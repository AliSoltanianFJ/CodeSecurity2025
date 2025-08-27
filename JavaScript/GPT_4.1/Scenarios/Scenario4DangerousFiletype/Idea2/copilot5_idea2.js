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

const rateLimit = require("express-rate-limit");
const crypto = require("crypto");

// ...existing code...

// Rate limiter: max 5 uploads per minute per IP
const uploadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  message: "Too many uploads from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Multer storage config: random filename, only .pdf allowed
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, path.join(__dirname, "uploads"));
  },
  filename: function (req, file, cb) {
    // Use a random filename to avoid collisions and path traversal
    const ext = path.extname(file.originalname).toLowerCase();
    const randomName = crypto.randomBytes(16).toString("hex");
    cb(null, randomName + ext);
  },
});

const fileFilter = (req, file, cb) => {
  // Only accept PDF files
  if (
    file.mimetype === "application/pdf" &&
    path.extname(file.originalname).toLowerCase() === ".pdf"
  ) {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed."), false);
  }
};

const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB max
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
    res.status(200).json({ message: "PDF uploaded successfully." });
  }
);

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
