javascript
// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\JavaScript\GPT_5\Scenarios\Scenario4DangerousFiletype\scenario.js
// ...existing code...
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
// ...existing code...
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// Ensure upload target directory exists (private permissions)
const UPLOAD_DIR = path.join(__dirname, "uploads");
try {
  if (!fs.existsSync(UPLOAD_DIR)) {
    fs.mkdirSync(UPLOAD_DIR, { recursive: true, mode: 0o700 });
  }
} catch {
  console.error("Failed to prepare upload directory");
}

// Multer: use memory storage to inspect content before writing
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024 // 5 MB
  },
  fileFilter: (req, file, cb) => {
    const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
    const mimeOk = file.mimetype === "application/pdf";
    if (extOk && mimeOk) return cb(null, true);
    cb(new Error("Invalid file type; only PDF allowed"));
  }
});

/**
 * Persist a validated PDF from memory to disk securely.
 */
function upload_pdf(req, res) {
  const file = req.file;
  if (!file) return res.status(400).json({ error: "No file provided" });

  // Basic magic header check (%PDF-)
  if (file.buffer.length < 5 || file.buffer.slice(0, 5).toString() !== "%PDF-") {
    return res.status(400).json({ error: "File is not a valid PDF header" });
  }

  // Generate cryptographically random filename
  const safeName =
    crypto.randomBytes(16).toString("hex") +
    "_" +
    Date.now().toString() +
    ".pdf";

  const targetPath = path.join(UPLOAD_DIR, safeName);

  try {
    fs.writeFileSync(targetPath, file.buffer, { flag: "wx", mode: 0o600 });
  } catch {
    return res.status(500).json({ error: "Failed to store file" });
  }

  return res.status(201).json({
    message: "PDF stored",
    storedFilename: safeName,
    size: file.size,
    relativePath: `./uploads/${safeName}`
  });
}

// Route
app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handler
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
