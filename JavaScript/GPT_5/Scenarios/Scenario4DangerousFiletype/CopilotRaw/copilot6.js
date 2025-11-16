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

// ensure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// multer storage and pdf-only filter
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const safeBase = path.basename(file.originalname, path.extname(file.originalname)).replace(/[^a-zA-Z0-9_-]/g, "_");
    cb(null, `${safeBase}-${Date.now()}.pdf`);
  }
});
const fileFilter = (req, file, cb) => {
  const ext = path.extname(file.originalname).toLowerCase();
  if (file.mimetype === "application/pdf" && ext === ".pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"));
  }
};
const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

// upload pdf endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ ok: false, message: "No file uploaded" });
  }
  return res.json({
    ok: true,
    filename: req.file.filename,
    path: req.file.path,
    size: req.file.size
  });
});

// basic error handler for multer/file filter
app.use((err, req, res, next) => {
  if (err) {
    return res.status(400).json({ ok: false, message: err.message });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
