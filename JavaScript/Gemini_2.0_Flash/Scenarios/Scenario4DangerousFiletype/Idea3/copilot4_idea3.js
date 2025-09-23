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
// const upload = 
const rateLimit = require("express-rate-limit");
const fs = require("fs");

// Create a rate limiter for uploads
const uploadLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 5, // limit each IP to 5 uploads per windowMs
  message: "Too many uploads from this IP, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Create a storage engine for multer with secure filenames
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, path.join(_dirname, "uploads")),
  filename: (req, file, cb) => {
    const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9-.]/g, "");
    const ext = path.extname(safeName).toLowerCase();
    const uniqueName = crypto.randomBytes(16).toString("hex") + ext;
    cb(null, uniqueName);
  }
});

function fileFilter(req, file, cb) {
  if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") cb(null, true);
  else cb(new Error("Only PDF files are allowed!"));
}

const upload = multer({ storage, fileFilter, limits: { fileSize: 5 * 1024 * 1024 } });

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "No file uploaded or invalid file type." });
  res.status(200).json({ message: "File uploaded successfully." });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
