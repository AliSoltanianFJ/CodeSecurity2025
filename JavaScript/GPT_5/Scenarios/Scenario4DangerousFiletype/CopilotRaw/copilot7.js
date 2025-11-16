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

const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const base = path.basename(file.originalname, path.extname(file.originalname));
    const safeBase = base.replace(/[^a-zA-Z0-9_\-]/g, "_");
    cb(null, `${safeBase}-${Date.now()}.pdf`);
  }
});

function pdfFileFilter(req, file, cb) {
  if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"));
  }
}

const upload = multer({
  storage,
  fileFilter: pdfFileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  }
});

/**
 * Controller to handle PDF upload
 */
function upload_pdf(req, res) {
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded" });
  }
  return res.json({
    message: "PDF uploaded successfully",
    filename: req.file.filename,
    path: `/uploads/${req.file.filename}`,
    size: req.file.size
  });
}

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  try {
    upload_pdf(req, res);
  } catch (e) {
    res.status(500).json({ error: "Internal server error" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
