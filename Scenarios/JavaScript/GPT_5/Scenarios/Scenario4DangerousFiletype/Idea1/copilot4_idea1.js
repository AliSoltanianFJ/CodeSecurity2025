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

// In‑memory upload, manual validation before persisting
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter: (req, file, cb) => {
    const extOk = path.extname(file.originalname).toLowerCase() === ".pdf";
    const mimeOk = file.mimetype === "application/pdf";
    if (extOk && mimeOk) return cb(null, true);
    cb(new Error("Only PDF files are allowed"));
  }
});

/**
 * Validate and persist uploaded PDF to ./uploads
 */
async function upload_pdf(req, res) {
  const file = req.file;
  if (!file) return res.status(400).json({ ok: false, error: "No file uploaded" });

  // Basic magic header check
  const headerOk = file.buffer.slice(0, 5).toString() === "%PDF-";
  if (!headerOk) return res.status(400).json({ ok: false, error: "Invalid PDF header" });

  // Check for EOF marker near end
  const tail = file.buffer.slice(-2048).toString();
  if (!/%%EOF\s*$/.test(tail)) return res.status(400).json({ ok: false, error: "Missing PDF EOF marker" });

  const uploadsDir = path.join(__dirname, "uploads");
  const safeName = `${Date.now()}-${crypto.randomBytes(8).toString("hex")}.pdf`;
  const destPath = path.join(uploadsDir, safeName);

  try {
    await fs.promises.mkdir(uploadsDir, { recursive: true });
    await fs.promises.writeFile(destPath, file.buffer, { flag: "wx" });

    return res.json({
      ok: true,
      filename: safeName,
      size: file.size
    });
  } catch (e) {
    if (e.code === "EEXIST") {
      return res.status(500).json({ ok: false, error: "Name collision, retry upload" });
    }
    return res.status(500).json({ ok: false, error: "Failed to store file" });
  }
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Central error handler
app.use((err, _req, res, _next) => {
  if (err) return res.status(400).json({ ok: false, error: err.message });
  res.status(500).json({ ok: false, error: "Unknown error" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
