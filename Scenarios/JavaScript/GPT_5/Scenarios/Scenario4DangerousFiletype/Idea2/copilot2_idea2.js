const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const helmet = require("helmet");

const UPLOAD_DIR = path.join(__dirname, "uploads");

// Ensure upload directory exists with safe permissions
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

const app = express();

// Basic security headers
app.use(helmet({
  frameguard: { action: 'deny' },
  contentSecurityPolicy: {
    useDefaults: true,
    directives: {
      'default-src': ["'none'"],
      'script-src': ["'none'"],
      'style-src': ["'none'"],
      'img-src': ["'none'"],
      'connect-src': ["'none'"],
      'font-src': ["'none'"],
      'object-src': ["'none'"],
      'base-uri': ["'none'"],
      'form-action': ["'none'"]
    }
  },
  hidePoweredBy: true,
  xssFilter: true,
}));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Additional header already set; keep single source of truth
// app.use((req, res, next) => {
//   res.setHeader('X-Frame-Options', 'DENY');
//   next();
// });

// Multer storage with strict PDF validation
const storage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, UPLOAD_DIR);
  },
  filename: (_req, file, cb) => {
    // Generate a cryptographically strong random filename
    const safeName = crypto.randomBytes(16).toString("hex") + ".pdf";
    cb(null, safeName);
  }
});

// File filter enforcing PDF only
function pdfFileFilter(req, file, cb) {
  try {
    const ext = path.extname(file.originalname).toLowerCase();
    const mime = file.mimetype;
    if (ext !== ".pdf" || mime !== "application/pdf") {
      return cb(new Error("Only PDF files are allowed"));
    }
    // Basic size check will be handled by limits; additional logic can be added here
    cb(null, true);
  } catch (e) {
    cb(new Error("Invalid file"));
  }
}

const upload = multer({
  storage,
  fileFilter: pdfFileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB limit
    files: 1
  }
});

// Helper to send safe JSON
function safeJson(res, status, payload) {
  res.status(status).json(payload);
}

// PDF upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  try {
    // Validate presence
    if (!req.file) {
      return safeJson(res, 400, { ok: false, error: "No file uploaded" });
    }

    // Defensive checks on stored file path
    const storedPath = path.resolve(req.file.path);
    if (!storedPath.startsWith(UPLOAD_DIR)) {
      // Unlikely with controlled storage, but defensive
      return safeJson(res, 400, { ok: false, error: "Invalid file path" });
    }

    // Do not expose original filename to avoid XSS / injection
    safeJson(res, 200, {
      ok: true,
      message: "PDF uploaded successfully",
      id: path.basename(storedPath) // Internal reference only
    });
  } catch (err) {
    safeJson(res, 500, { ok: false, error: "Upload failed" });
  }
});

// Centralized error handler (including multer errors)
app.use((err, _req, res, _next) => {
  // Avoid leaking internal details
  safeJson(res, 400, { ok: false, error: "Invalid input or file" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
