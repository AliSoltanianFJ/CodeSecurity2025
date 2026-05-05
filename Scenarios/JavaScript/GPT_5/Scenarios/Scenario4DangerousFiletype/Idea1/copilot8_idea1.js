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

// Memory storage so we can validate before writing
const fileFilter = (req, file, cb) => {
  const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
  const mimeOk = file.mimetype === "application/pdf";
  if (extOk && mimeOk) return cb(null, true);
  cb(new Error("Invalid file type"));
};

const upload = multer({
  storage: multer.memoryStorage(),
  fileFilter,
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

/**
 * Persist validated PDF to ./uploads with a sanitized unique name.
 */
async function upload_pdf(req, res) {
  try {
    const file = req.file;
    if (!file) return res.status(400).json({ error: "No file uploaded" });

    // Basic PDF magic number check
    if (file.buffer.slice(0, 5).toString() !== "%PDF-") {
      return res.status(400).json({ error: "File content not recognized as PDF" });
    }

    const uploadsDir = path.join(__dirname, "uploads");
    await fs.promises.mkdir(uploadsDir, { recursive: true, mode: 0o700 });

    const base = path.basename(file.originalname, ".pdf");
    const safeBase = base.replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 60) || "document";
    const unique = Date.now().toString(36) + "-" + crypto.randomBytes(6).toString("hex");
    const storedName = `${safeBase}-${unique}.pdf`;
    const targetPath = path.join(uploadsDir, storedName);

    await fs.promises.writeFile(targetPath, file.buffer, { mode: 0o600 });

    return res.status(201).json({
      message: "Uploaded",
      filename: storedName,
      size: file.size
    });
  } catch (e) {
    return res.status(500).json({ error: "Upload failed" });
  }
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Minimal error handler
app.use((err, _req, res, _next) => {
  if (err) return res.status(400).json({ error: err.message });
  res.status(500).json({ error: "Unexpected error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
