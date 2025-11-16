
const express = require("express");
const mysql = require("mysql"); // (unused; consider removing if not needed)
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
// ...existing code...
const app = express();
// Minimal JSON/body parsing limits to mitigate resource exhaustion
app.use(express.json({ limit: "50kb" }));
app.use(express.urlencoded({ extended: false, limit: "50kb" }));
// ...existing code...
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("X-XSS-Protection", "0"); // Modern browsers; rely on proper output encoding
  next();
});
// ...existing code...
// Ensure uploads directory exists safely
const uploadsDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB limit

function sanitizeFilename(name) {
  // Keep only safe characters
  return name.replace(/[^a-zA-Z0-9._-]/g, "_");
}

// Multer storage configuration with strict validation
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Force .pdf extension, ignore supplied extension
    const randomName = crypto.randomBytes(16).toString("hex");
    cb(null, randomName + ".pdf");
  }
});

function fileFilter(req, file, cb) {
  // Validate MIME and original extension
  const ext = path.extname(file.originalname || "").toLowerCase();
  if (file.mimetype !== "application/pdf" || ext !== ".pdf") {
    return cb(new Error("Only genuine PDF files are allowed"));
  }
  cb(null, true);
}

const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: MAX_FILE_SIZE,
    files: 1
  }
});

// Centralized error handler
function safeError(message, status = 400) {
  const err = new Error(message);
  err.status = status;
  return err;
}

// ...existing code...
/**
 * Save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res, next) => {
  try {
    if (!req.file) {
      throw safeError("No file uploaded");
    }

    // Sanitize original name for any logging or response (not used for storage)
    const originalName = sanitizeFilename(req.file.originalname || "unnamed.pdf");

    // Double check stored file path is inside uploadsDir
    const storedPath = path.resolve(req.file.path);
    if (!storedPath.startsWith(uploadsDir)) {
      // Defensive: should never happen if diskStorage is correct
      fs.unlink(storedPath, () => {});
      throw safeError("Invalid storage path");
    }

    // Basic PDF header validation (lightweight check)
    try {
      const fd = fs.openSync(storedPath, "r");
      const buf = Buffer.alloc(5);
      fs.readSync(fd, buf, 0, 5, 0);
      fs.closeSync(fd);
      if (buf.toString() !== "%PDF-") {
        fs.unlinkSync(storedPath);
        throw safeError("File content not a valid PDF");
      }
    } catch (e) {
      if (e.status) throw e;
      throw safeError("Failed to validate PDF");
    }

    res.status(200).json({
      message: "PDF uploaded successfully",
      filename: path.basename(req.file.filename),
      originalName
    });
  } catch (err) {
    next(err);
  }
});

// Error handler (last middleware)
app.use((err, req, res, next) => {
  const status = err.status || 500;
  // Generic message to avoid leaking internals
  res.status(status).json({ error: status === 500 ? "Internal error" : err.message });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
