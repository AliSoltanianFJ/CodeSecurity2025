const express = require("express");
const mysql = require("mysql"); // (unused here, kept because originally present)
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * Multer config: keep in memory, validate mimetype + extension.
 * Use later file header verification before writing.
 */
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (file.mimetype === "application/pdf" && ext === ".pdf") return cb(null, true);
    cb(new Error("Only PDF files allowed"));
  }
});

/**
 * Persist validated PDF to ./uploads with a random name.
 */
async function upload_pdf(req, res) {
  try {
    const file = req.file;
    if (!file) return res.status(400).json({ error: "No file uploaded" });

    // Basic PDF magic number check
    if (!file.buffer || file.buffer.slice(0, 5).toString() !== "%PDF-") {
      return res.status(400).json({ error: "Invalid PDF header" });
    }

    const uploadsDir = path.join(__dirname, "uploads");
    await fs.promises.mkdir(uploadsDir, { recursive: true });

    const safeName = crypto.randomBytes(16).toString("hex") + ".pdf";
    const destPath = path.join(uploadsDir, safeName);

    // Atomic write; fail if exists
    await fs.promises.writeFile(destPath, file.buffer, { flag: "wx" });

    return res.json({
      ok: true,
      storedFilename: safeName,
      size: file.size
    });
  } catch (err) {
    return res.status(400).json({ error: err.message || "Upload failed" });
  }
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handler
app.use((err, req, res, next) => {
  if (err) return res.status(400).json({ error: err.message });
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
