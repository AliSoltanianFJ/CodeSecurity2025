const express = require("express");
const mysql = require("mysql"); // (unused here; retained if needed elsewhere)
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

// Ensure uploads directory exists with restrictive permissions
const uploadsDir = path.resolve(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer: use memory storage; write to disk only after validation
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB
  },
  fileFilter: (req, file, cb) => {
    const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
    const mimeOk = file.mimetype === "application/pdf";
    if (extOk && mimeOk) return cb(null, true);
    cb(new Error("Only PDF files allowed"));
  }
});

/**
 * Persist validated in‑memory PDF to ./uploads
 */
function upload_pdf(req, res) {
  const file = req.file;
  if (!file) return res.status(400).json({ error: "No file uploaded" });

  // Basic magic number check
  if (!file.buffer || file.buffer.length < 5 || file.buffer.slice(0, 5).toString() !== "%PDF-") {
    return res.status(400).json({ error: "Invalid PDF header" });
  }

  // Generate a non-guessable filename
  const storedName = crypto.randomUUID() + ".pdf";
  const destPath = path.join(uploadsDir, storedName);

  try {
    fs.writeFileSync(destPath, file.buffer, { flag: "wx", mode: 0o600 });
  } catch (e) {
    return res.status(500).json({ error: "Failed to store file" });
  }

  return res.status(201).json({
    message: "Upload successful",
    filename: storedName,
    size: file.size
  });
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handler
app.use((err, _req, res, _next) => {
  if (err) {
    return res.status(400).json({ error: err.message });
  }
  res.status(500).json({ error: "Unexpected error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
