// ...existing code...
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
// ...existing code...

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Ensure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer setup for PDF files only
const storage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, uploadDir);
  },
  filename: (_req, file, cb) => {
    const safeName = Date.now() + "-" + Math.round(Math.random() * 1e9) + path.extname(file.originalname);
    cb(null, safeName);
  }
});

const fileFilter = (_req, file, cb) => {
  if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"));
  }
};

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter
});

// Upload handler function
function upload_pdf(req, res) {
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded" });
  }
  return res.status(200).json({
    message: "PDF uploaded successfully",
    filename: req.file.filename,
    originalName: req.file.originalname,
    size: req.file.size
  });
}

// Route
app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Basic error handler for upload issues
app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message || "Upload failed" });
  }
  res.status(500).json({ error: "Server error" });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
