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
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// Secure memory storage: write only after validation
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  },
  fileFilter: (req, file, cb) => {
    const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
    const mimeOk = file.mimetype === "application/pdf";
    if (extOk && mimeOk) return cb(null, true);
    cb(new Error("Only PDF files are allowed"));
  }
});

// Ensure uploads directory exists (lazy creation)
async function ensureUploadsDir() {
  const dir = path.join(__dirname, "uploads");
  await fs.promises.mkdir(dir, { recursive: true });
  return dir;
}

/**
 * Save validated PDF from memory buffer to disk
 */
async function upload_pdf(req, res, next) {
  try {
    const file = req.file;
    if (!file) {
      return res.status(400).json({ error: "No file uploaded" });
    }

    // Basic magic number check
    if (file.buffer.length < 5 || file.buffer.slice(0, 5).toString() !== "%PDF-") {
      return res.status(400).json({ error: "Invalid PDF header" });
    }

    const uploadsDir = await ensureUploadsDir();

    const originalBase = path.basename(file.originalname, path.extname(file.originalname));
    const safeBase = originalBase.replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 80) || "document";
    const unique = crypto.randomUUID();
    const storedName = `${safeBase}-${unique}.pdf`;
    const targetPath = path.join(uploadsDir, storedName);

    await fs.promises.writeFile(targetPath, file.buffer, { mode: 0o600 });

    return res.status(201).json({
      ok: true,
      storedName,
      size: file.size,
      path: `/uploads/${storedName}`
    });
  } catch (err) {
    return next(err);
  }
}

// ...existing code...
app.post("/pdf/upload", upload.single("pdf"), upload_pdf);
// ...existing code...

app.use((err, req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message || "Upload failed" });
  }
  res.status(500).json({ error: "Unexpected error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
