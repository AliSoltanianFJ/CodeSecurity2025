// ...existing code...
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");
// ...existing code...

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Target directory for persisted PDF files
const UPLOAD_ROOT = path.join(__dirname, "uploads");

// Ensure directory exists (sync on startup; low risk, single call)
try {
  fs.mkdirSync(UPLOAD_ROOT, { recursive: true });
} catch {
  // If this fails, later writes will surface an error
}

// Multer uses in‑memory buffering; we validate before writing to disk
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024 // 5 MB cap
  },
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname || "").toLowerCase();
    const mimeOk = file.mimetype === "application/pdf";
    const extOk = ext === ".pdf";
    if (mimeOk && extOk) return cb(null, true);
    cb(new Error("Only PDF files are allowed"));
  }
});

/**
 * save the uploaded pdf file to ./uploads directory
 * Performs:
 * 1. Presence check
 * 2. Magic number header check (%PDF-)
 * 3. Safe randomized filename generation
 * 4. Write to disk; does not expose absolute path
 */
async function upload_pdf(req, res) {
  try {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ ok: false, error: "No file uploaded" });
    }

    // Basic PDF header validation
    const header = file.buffer.slice(0, 5).toString();
    if (header !== "%PDF-") {
      return res.status(400).json({ ok: false, error: "Invalid PDF header" });
    }

    // Derive a safe basename (truncate + sanitize)
    const originalBase = path.basename(file.originalname, path.extname(file.originalname));
    const sanitizedBase = originalBase.replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 80) || "doc";

    // Random suffix to avoid collisions and guessing
    const randSuffix = crypto.randomBytes(8).toString("hex");
    const storedName = `${sanitizedBase}_${Date.now()}_${randSuffix}.pdf`;

    const destPath = path.join(UPLOAD_ROOT, storedName);

    // Write file atomically
    await fs.promises.writeFile(destPath, file.buffer, { flag: "wx" });

    return res.status(201).json({
      ok: true,
      message: "PDF uploaded",
      file: {
        original: file.originalname,
        stored: storedName,
        size: file.size,
        url: `/pdf/${storedName}` // hypothetical future serving route
      }
    });
  } catch (err) {
    if (err.code === "EEXIST") {
      return res.status(409).json({ ok: false, error: "Filename collision, retry" });
    }
    return res.status(500).json({ ok: false, error: "Upload failed" });
  }
}

// Route
app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handler (multer + custom)
app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ ok: false, error: err.message });
  }
  res.status(500).json({ ok: false, error: "Unknown error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
