// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */

const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const rateLimit = require("express-rate-limit");
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
  message: "Too many upload attempts, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Multer storage config with strict file filter
const upload = multer({
  storage: multer.diskStorage({
    destination: function (req, file, cb) {
      const uploadDir = path.join(__dirname, "uploads");
      if (!fs.existsSync(uploadDir)) {
        fs.mkdirSync(uploadDir, { recursive: true });
      }
      cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
      // Only allow .pdf extension, sanitize filename
      const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\-\.]/g, "");
      const ext = path.extname(safeName).toLowerCase();
      const base = path.basename(safeName, ext).slice(0, 50); // limit length
      const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
      cb(null, base + '-' + uniqueSuffix + ext);
    }
  }),
  fileFilter: function (req, file, cb) {
    // Only accept PDF files
    if (
      file.mimetype === "application/pdf" &&
      path.extname(file.originalname).toLowerCase() === ".pdf"
    ) {
      cb(null, true);
    } else {
      cb(new Error("Only PDF files are allowed!"));
    }
  },
  limits: {
    fileSize: 5 * 1024 * 1024 // 5 MB max
  }
});

// ---existing code---

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: "No file uploaded or invalid file type." });
    }
    // Double check file extension and mimetype
    if (
      file.mimetype !== "application/pdf" ||
      path.extname(file.originalname).toLowerCase() !== ".pdf"
    ) {
      // Remove file if it slipped through
      fs.unlink(file.path, () => {});
      return res.status(400).json({ error: "Invalid file type." });
    }
    res.status(200).json({ message: "PDF uploaded successfully.", filename: file.filename });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
