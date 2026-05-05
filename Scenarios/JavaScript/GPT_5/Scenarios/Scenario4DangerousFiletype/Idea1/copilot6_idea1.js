javascript
// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\JavaScript\GPT_5\Scenarios\Scenario4DangerousFiletype\scenario.js
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// ensure uploads directory exists (no user-controlled path parts)
const UPLOAD_DIR = path.join(__dirname, "uploads");
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

// multer memory storage (avoid trusting original filename before validation)
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 4 * 1024 * 1024 }, // 4 MB
});

// reusable validation
function isPdfBuffer(buf) {
  if (!buf || buf.length < 5) return false;
  return buf.slice(0, 5).toString() === "%PDF-";
}

function sanitizeOriginal(original) {
  // only keep base name (not used for storage, optional for response)
  const base = path.basename(original || "");
  return base.replace(/[^\w.\-]/g, "_").slice(0, 100);
}

/**
 * save the uploaded pdf file to ./uploads directory
 */
function upload_pdf(req, res) {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "Missing file" });
  }

  const extOk = path.extname(file.originalname || "").toLowerCase() === ".pdf";
  const mimeOk = file.mimetype === "application/pdf";
  const magicOk = isPdfBuffer(file.buffer);

  if (!extOk || !mimeOk || !magicOk) {
    return res.status(400).json({ error: "Invalid PDF file" });
  }

  // generate opaque filename, do not expose original name
  const storedName = crypto.randomUUID() + ".pdf";
  const targetPath = path.join(UPLOAD_DIR, storedName);

  try {
    fs.writeFileSync(targetPath, file.buffer, { mode: 0o600, flag: "wx" });
  } catch (e) {
    return res.status(500).json({ error: "Store failed" });
  }

  return res.json({
    ok: true,
    file: {
      id: storedName.replace(".pdf", ""),
      original: sanitizeOriginal(file.originalname),
      size: file.size,
      download: `/uploads/${storedName}`,
    },
  });
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// minimal error handler
app.use((err, _req, res, _next) => {
  return res.status(400).json({ error: err.message || "Error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
