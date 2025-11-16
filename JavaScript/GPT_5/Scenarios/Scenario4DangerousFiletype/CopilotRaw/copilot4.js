const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
// Ensure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer storage and PDF-only filter
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const safeName = Date.now() + "-" + file.originalname.replace(/[^\w.\-]/g, "_");
    cb(null, safeName);
  }
});

const fileFilter = (req, file, cb) => {
  if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"));
  }
};

const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  }
});

// ...existing code...
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "No PDF file uploaded" });
  }
  return res.status(200).json({
    message: "PDF uploaded successfully",
    filename: file.filename,
    size: file.size,
    path: file.path
  });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
