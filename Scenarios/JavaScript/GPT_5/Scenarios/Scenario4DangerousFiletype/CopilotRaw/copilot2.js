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
// ...existing code...
// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, uploadDir),
  filename: (_req, file, cb) => {
    const base = path.basename(file.originalname, path.extname(file.originalname));
    const safeBase = base.replace(/[^a-zA-Z0-9_\-]/g, "_");
    const timestamp = Date.now();
    cb(null, `${safeBase}_${timestamp}.pdf`);
  }
});

function fileFilter(_req, file, cb) {
  const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
  const mimeOk = file.mimetype === "application/pdf";
  if (extOk && mimeOk) return cb(null, true);
  cb(new Error("Only PDF files are allowed"));
}

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "No file uploaded" });
  }
  res.json({
    message: "Upload successful",
    filename: file.filename,
    path: `/uploads/${file.filename}`
  });
});

app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message });
  }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
