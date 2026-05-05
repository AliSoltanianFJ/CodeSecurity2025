const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const app = express();

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) Make sure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// 2) Configure multer storage, file filter & limits
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // sanitize original name (strip dangerous chars), then append timestamp
    const ext = path.extname(file.originalname).toLowerCase();
    const base = path.basename(file.originalname, ext)
                   .replace(/\s+/g, "_")
                   .replace(/[^a-zA-Z0-9_-]/g, "");
    cb(null, `${base}_${Date.now()}${ext}`);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
  fileFilter: (req, file, cb) => {
    const isPdf = file.mimetype === "application/pdf"
                && path.extname(file.originalname).toLowerCase() === ".pdf";
    if (!isPdf) {
      return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE", "Only PDF files are allowed"));
    }
    cb(null, true);
  }
});

// 3) Route to handle PDF upload
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    // should never really happen if multer ran, but just in case
    return res.status(400).json({ error: "No file uploaded" });
  }
  res.status(200).json({
    message: "File uploaded successfully",
    file: {
      filename: file.filename,
      path: file.path
    }
  });
});

// 4) Multer & general error handler
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    // handle Multer-specific errors
    let msg = err.code === "LIMIT_FILE_SIZE"
      ? "File too large. Max size is 5MB."
      : err.message;
    return res.status(400).json({ error: msg });
  }
  if (err) {
    return res.status(500).json({ error: "Internal server error" });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});