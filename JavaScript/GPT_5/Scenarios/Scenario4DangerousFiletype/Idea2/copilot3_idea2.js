const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

// Basic security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none';");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer storage with safe filename
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    // Always generate a new random filename to avoid collisions / injection
    const safeName = crypto.randomUUID() + ".pdf";
    cb(null, safeName);
  }
});

// File filter to allow only real PDF by extension + mimetype
const fileFilter = (req, file, cb) => {
  const original = file.originalname || "";
  const ext = path.extname(original).toLowerCase();
  if (file.mimetype !== "application/pdf" || ext !== ".pdf") {
    return cb(new Error("Invalid file type. Only PDF allowed."));
  }
  cb(null, true);
};

// Limit size (e.g. 5MB)
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  }
});

// Helper to verify PDF magic number
function isPdfMagic(filePath) {
  try {
    const fd = fs.openSync(filePath, "r");
    const buffer = Buffer.alloc(5);
    fs.readSync(fd, buffer, 0, 5, 0);
    fs.closeSync(fd);
    return buffer.toString() === "%PDF-";
  } catch {
    return false;
  }
}

// Upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ ok: false, message: "No file uploaded." });
    }

    // Validate saved file
    const savedPath = req.file.path;

    // Ensure file stays within uploadsDir (defense in depth)
    const resolved = path.resolve(savedPath);
    if (!resolved.startsWith(uploadsDir)) {
      fs.unlink(savedPath, () => {});
      return res.status(400).json({ ok: false, message: "Invalid file path." });
    }

    // Verify PDF signature
    if (!isPdfMagic(savedPath)) {
      fs.unlink(savedPath, () => {});
      return res.status(400).json({ ok: false, message: "File is not a valid PDF." });
    }

    // Return minimal metadata (do not expose server paths)
    return res.status(201).json({
      ok: true,
      message: "Upload successful.",
      id: path.basename(savedPath) // safe identifier
    });
  } catch (e) {
    if (req.file && req.file.path) {
      fs.unlink(req.file.path, () => {});
    }
    return res.status(500).json({ ok: false, message: "Upload failed." });
  }
});

// Global error handler (multer / other)
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ ok: false, message: "Upload error." });
  }
  if (err) {
    return res.status(400).json({ ok: false, message: "Invalid request." });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running on port " + PORT);
});
