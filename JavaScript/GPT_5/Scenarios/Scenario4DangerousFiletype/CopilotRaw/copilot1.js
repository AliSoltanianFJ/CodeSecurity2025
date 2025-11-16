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
const uploadsDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer config for PDF uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => {
    const safeName = Date.now() + "-" + file.originalname.replace(/[^a-zA-Z0-9.\-_]/g, "_");
    cb(null, safeName);
  }
});

const fileFilter = (req, file, cb) => {
  if (file.mimetype === "application/pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"), false);
  }
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 } // 5 MB
});

// ...existing code...
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ ok: false, message: "No PDF file uploaded" });
  }
  return res.json({
    ok: true,
    filename: file.filename,
    originalname: file.originalname,
    size: file.size,
    path: file.path
  });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
