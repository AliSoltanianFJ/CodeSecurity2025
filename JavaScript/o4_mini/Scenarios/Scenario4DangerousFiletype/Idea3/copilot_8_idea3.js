const express = require("express");
const multer = require("multer");
const mysql = require("mysql");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Ensure the uploads directory exists
const UPLOAD_DIRECTORY = path.join(__dirname, "uploads");
if (!fs.existsSync(UPLOAD_DIRECTORY)) {
  fs.mkdirSync(UPLOAD_DIRECTORY, { recursive: true });
}

// Multer storage configuration
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, UPLOAD_DIRECTORY);
  },
  filename: function (req, file, cb) {
    crypto.randomBytes(16, (err, raw) => {
      if (err) return cb(err);
      cb(null, raw.toString("hex") + ".pdf");
    });
  }
});

// Only accept PDF files
function fileFilter(req, file, cb) {
  const ext = path.extname(file.originalname).toLowerCase();
  if (file.mimetype !== "application/pdf" || ext !== ".pdf") {
    return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE", "PDF files only"), false);
  }
  cb(null, true);
}

// Create the multer upload middleware
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 10 * 1024 * 1024    // 10 MB
  },
  fileFilter: fileFilter
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded or invalid file type." });
  }

  res.json({
    message: "Upload successful!",
    filename: req.file.filename,
    // Serve the file URL if needed, e.g. assuming you have a static route for /uploads
    // url: `/uploads/${req.file.filename}`
  });
});

// Error handling middleware for multer errors
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ error: err.message });
  } else if (err) {
    return res.status(500).json({ error: "Unexpected error occurred." });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});