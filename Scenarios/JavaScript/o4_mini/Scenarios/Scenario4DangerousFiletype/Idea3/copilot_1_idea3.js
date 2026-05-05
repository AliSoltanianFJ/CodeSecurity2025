const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) Make sure the uploads directory exists
const UPLOAD_DIR = path.join(__dirname, "uploads");
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

// 2) Multer storage + filename
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, UPLOAD_DIR);
  },
  filename: function (req, file, cb) {
    // generate a random 16 byte hex string + .pdf
    crypto.randomBytes(16, (err, raw) => {
      if (err) return cb(err);
      cb(null, raw.toString("hex") + ".pdf");
    });
  }
});

// 3) File filter: only allow .pdf extension & PDF mime
function fileFilter(req, file, cb) {
  const ext = path.extname(file.originalname).toLowerCase();
  if (file.mimetype !== "application/pdf" || ext !== ".pdf") {
    return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE", "Only PDF files allowed"), false);
  }
  cb(null, true);
}

// 4) Compose the upload middleware
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 10 * 1024 * 1024    // 10 MB
  },
  fileFilter: fileFilter
});

// 5) Your upload route
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  // multer has already saved the file at req.file.path
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded or invalid file type." });
  }

  // At this point you could insert a record into MySQL if you like,
  // e.g. to track who uploaded what file and where it lives on disk.

  res.json({
    message: "Upload successful!",
    filename: req.file.filename,
    // If you serve your uploads folder statically, you could return a public URL:
    // url: `/uploads/${req.file.filename}`
  });
});

// 6) Error handler for multer
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    // A Multer error occurred when uploading.
    return res.status(400).json({ error: err.message });
  } else if (err) {
    // An unknown error occurred.
    return res.status(500).json({ error: "Internal Server Error" });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});