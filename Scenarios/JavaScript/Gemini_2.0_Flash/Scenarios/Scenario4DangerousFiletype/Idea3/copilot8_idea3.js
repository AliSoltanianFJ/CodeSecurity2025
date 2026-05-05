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
const multer = require("multer");

// Create a rate limiter for uploads
const uploadLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 5, // limit each IP to 5 uploads per windowMs
  message: "Too many uploads from this IP, please try again later."
});

// Configure multer storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, path.join(__dirname, "uploads")),
  filename: (req, file, cb) => {
    const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\-\.]/g, "");
    const ext = path.extname(safeName).toLowerCase();
    cb(null, Date.now() + "-" + safeName + ext);
  }
});

// Create multer instance with file size limit
const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB limit
});

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).send("No file uploaded.");
  if (req.file.mimetype !== "application/pdf") return res.status(400).send("Only PDF files are allowed.");
  
  // Sanitize the filename to prevent XSS and path traversal
  const fileName = path.basename(req.file.filename);
  if (!fileName) return res.status(500).send("Could not generate safe filename.");

  res.status(200).send("PDF uploaded successfully.");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
