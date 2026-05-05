
const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
const helmet = require("helmet");

const app = express();
app.use(helmet({
  crossOriginEmbedderPolicy: false
}));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  next();
});

const UPLOAD_DIR = path.join(__dirname, "uploads");
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

// Multer setup: memory storage for content inspection before writing
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  },
  fileFilter: (req, file, cb) => {
    // Enforce PDF by MIME and extension
    const ext = path.extname(file.originalname || '').toLowerCase();
    if (file.mimetype !== 'application/pdf' || ext !== '.pdf') {
      return cb(new Error('Invalid file type. Only PDF allowed.'));
    }
    cb(null, true);
  }
});

/**
 * POST /pdf/upload
 * Accepts a single PDF file field named "pdf"
 * Validates signature and stores safely.
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "No file uploaded." });
    }

    // Basic content signature check (%PDF- at start)
    const buf = req.file.buffer;
    const header = buf.slice(0, 5).toString('utf8');
    if (header !== '%PDF-') {
      return res.status(400).json({ error: "File content is not a valid PDF." });
    }

    // Sanitize original name
    const originalBase = path.basename(req.file.originalname, '.pdf');
    const safeBase = originalBase.replace(/[^a-zA-Z0-9_\-]/g, '').substring(0, 50) || 'document';
    const uniqueSuffix = crypto.randomBytes(12).toString('hex');
    const finalName = `${safeBase}_${uniqueSuffix}.pdf`;

    // Absolute, safe path
    const destPath = path.join(UPLOAD_DIR, finalName);

    // Write file atomically
    fs.writeFile(destPath, buf, { flag: 'wx' }, (err) => {
      if (err) {
        return res.status(500).json({ error: "Failed to store file." });
      }
      return res.status(201).json({
        message: "Upload successful.",
        filename: finalName,
        sizeBytes: buf.length
      });
    });
  } catch (e) {
    return res.status(500).json({ error: "Internal error." });
  }
});

app.use((err, req, res, next) => {
  // Central error handler (avoid leaking details)
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ error: "Upload error." });
  }
  if (err) {
    return res.status(400).json({ error: "Invalid request." });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
