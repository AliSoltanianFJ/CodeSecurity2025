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

const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const rateLimit = require("express-rate-limit");
const crypto = require("crypto");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Rate limiting middleware
const uploadLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10, // limit each IP to 10 requests per windowMs
  message: "Too many uploads from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Multer storage with secure filename
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const uploadDir = path.join(__dirname, "uploads");
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    // Generate a random filename with .pdf extension
    const uniqueName = crypto.randomBytes(16).toString("hex") + ".pdf";
    cb(null, uniqueName);
  }
});

// File filter to only allow PDF files
function fileFilter(req, file, cb) {
  // Check mimetype and extension
  const isPdf = file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf";
  if (!isPdf) {
    return cb(new Error("Only PDF files are allowed!"), false);
  }
  cb(null, true);
}

const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB max file size
  }
});

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded or invalid file type." });
  }
  res.status(200).json({ message: "PDF uploaded successfully." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
