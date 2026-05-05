const express = require("express");
const mysql = require("mysql"); // (unused here; kept because in original file)
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Ensure ./uploads directory exists (minimal, no broad perms)
const uploadRoot = path.join(__dirname, "uploads");
try {
  fs.mkdirSync(uploadRoot, { recursive: true });
} catch { /* ignore */ }

// Use in‑memory storage; write to disk only after validation
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 } // 5 MB
});

/**
 * Validate and persist a PDF uploaded in memory.
 * Requirements:
 *  - Must have .pdf extension (case-insensitive)
 *  - Must start with "%PDF-" magic header
 *  - Sanitized filename; random suffix to avoid collisions
 */
function upload_pdf(req, res) {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "No file provided" });
  }

  const original = file.originalname || "";
  const ext = path.extname(original).toLowerCase();
  if (ext !== ".pdf") {
    return res.status(400).json({ error: "Invalid extension (only .pdf)" });
  }

  // Basic magic number check
  if (file.buffer.length < 5 || file.buffer.slice(0, 5).toString() !== "%PDF-") {
    return res.status(400).json({ error: "File content not recognized as PDF" });
  }

  // Sanitize base name
  const base = path.basename(original, ext)
    .replace(/[^a-zA-Z0-9_\-]/g, "_")
    .slice(0, 80) || "document";

  // Create a cryptographically random suffix
  const suffix = crypto.randomBytes(8).toString("hex");
  const storedName = `${base}-${suffix}.pdf`;
  const targetPath = path.join(uploadRoot, storedName);

  try {
    // Refuse overwrite if somehow exists
    if (fs.existsSync(targetPath)) {
      return res.status(409).json({ error: "Name collision; retry upload" });
    }
    fs.writeFileSync(targetPath, file.buffer, { flag: "wx" });
  } catch (e) {
    return res.status(500).json({ error: "Failed to store file" });
  }

  return res.status(201).json({
    message: "PDF stored",
    storedName,
    size: file.size
  });
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Minimal error handler
app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message || "Upload error" });
  }
  res.status(500).json({ error: "Unexpected error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
