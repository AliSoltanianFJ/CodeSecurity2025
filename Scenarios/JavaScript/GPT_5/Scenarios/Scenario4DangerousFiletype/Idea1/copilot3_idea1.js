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

// Prepare ./uploads directory (private perms)
const uploadDir = path.resolve(__dirname, "uploads");
fs.mkdirSync(uploadDir, { recursive: true });

// Multer configuration (PDF only)
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    // Ignore user filename; generate opaque name
    cb(null, crypto.randomUUID() + ".pdf");
  }
});

function pdfFilter(req, file, cb) {
  const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
  const mimeOk = file.mimetype === "application/pdf";
  if (extOk && mimeOk) return cb(null, true);
  cb(new Error("Invalid file type"));
}

const upload = multer({
  storage,
  fileFilter: pdfFilter,
  limits: { fileSize: 4 * 1024 * 1024 } // 4MB
});

/**
 * Validate uploaded PDF (basic header) and respond.
 */
async function upload_pdf(req, res, next) {
  const file = req.file;
  if (!file) return res.status(400).json({ error: "No file uploaded" });

  // Basic signature check (%PDF-)
  try {
    const fd = await fs.promises.open(file.path, "r");
    const buf = Buffer.alloc(5);
    await fd.read(buf, 0, 5, 0);
    await fd.close();
    if (buf.toString() !== "%PDF-") {
      await fs.promises.unlink(file.path);
      return res.status(400).json({ error: "File content not recognized as PDF" });
    }
  } catch (e) {
    return next(new Error("File inspection failed"));
  }

  return res.json({
    ok: true,
    storedName: path.basename(file.path),
    size: file.size
  });
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handling
app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message || "Upload error" });
  }
  res.status(500).json({ error: "Unknown error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
