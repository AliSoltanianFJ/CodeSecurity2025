const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none';");
  res.setHeader("X-XSS-Protection", "0");
  next();
});

// Ensure uploads directory exists with strict permissions.
const uploadsDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
  try {
    fs.chmodSync(uploadsDir, 0o700);
  } catch (_) { /* ignore chmod errors on some fs */ }
}

// Multer storage with safe filename generation.
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Generate a cryptographically strong random filename.
    const safeName = crypto.randomUUID() + ".pdf";
    cb(null, safeName);
  }
});

// File filter to allow only proper PDF.
function pdfFileFilter(req, file, cb) {
  const originalName = file.originalname || "";
  const ext = path.extname(originalName).toLowerCase();
  const mimeOk = file.mimetype === "application/pdf";
  const extOk = ext === ".pdf";
  if (!mimeOk || !extOk) {
    return cb(new Error("Invalid file type. Only PDF allowed."));
  }
  cb(null, true);
}

// Limit size (e.g. 5 MB) to mitigate resource exhaustion.
const upload = multer({
  storage,
  fileFilter: pdfFileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5 MB
    files: 1
  }
});

// Centralized safe JSON response helper.
function safeJson(res, statusCode, payload) {
  res.status(statusCode).type("application/json").send(JSON.stringify(payload));
}

// Upload handler.
function uploadPdfHandler(req, res) {
  // At this point multer processed the file if valid.
  const file = req.file;
  if (!file) {
    return safeJson(res, 400, { success: false, error: "No file uploaded." });
  }

  // Extra sanity checks.
  if (file.size <= 0) {
    // Remove empty file.
    try { fs.unlinkSync(file.path); } catch (_) {}
    return safeJson(res, 400, { success: false, error: "Empty file." });
  }

  // Respond with minimal metadata.
  safeJson(res, 201, {
    success: true,
    message: "PDF uploaded.",
    filename: file.filename,
    size: file.size
  });
}

// Route definition.
app.post("/pdf/upload", (req, res, next) => {
  upload.single("pdf")(req, res, (err) => {
    if (err) {
      // Generic error sanitization.
      return safeJson(res, 400, { success: false, error: "Upload failed." });
    }
    try {
      uploadPdfHandler(req, res);
    } catch (_) {
      safeJson(res, 500, { success: false, error: "Internal error." });
    }
  });
});

// Global error handler (final safeguard).
app.use((err, req, res, next) => {
  safeJson(res, 500, { success: false, error: "Server error." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
