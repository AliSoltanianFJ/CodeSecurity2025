const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Ensure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// Multer storage and filtering for PDFs only
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Normalize and randomize filename
    const base = path.basename(file.originalname, path.extname(file.originalname));
    const safeBase = base.replace(/[^a-zA-Z0-9_\-]/g, "_").slice(0, 100);
    const unique = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, `${safeBase}-${unique}.pdf`);
  }
});

function isPdf(mimetype, originalName) {
  const ext = path.extname(originalName).toLowerCase();
  return mimetype === "application/pdf" && ext === ".pdf";
}

const upload = multer({
  storage,
  fileFilter: (req, file, cb) => {
    if (isPdf(file.mimetype, file.originalname)) {
      cb(null, true);
    } else {
      cb(new Error("Only PDF files are allowed"));
    }
  },
  limits: {
    fileSize: 5 * 1024 * 1024 // 5 MB
  }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ ok: false, error: "No file uploaded or invalid file type" });
  }

  // (Optional) Basic magic number check for PDF (%PDF-)
  try {
    const fd = fs.openSync(file.path, "r");
    const buffer = Buffer.alloc(5);
    fs.readSync(fd, buffer, 0, 5, 0);
    fs.closeSync(fd);
    if (buffer.toString() !== "%PDF-") {
      fs.unlinkSync(file.path);
      return res.status(400).json({ ok: false, error: "File content is not a valid PDF header" });
    }
  } catch (e) {
    return res.status(500).json({ ok: false, error: "File validation failed" });
  }

  return res.json({
    ok: true,
    message: "PDF uploaded successfully",
    filename: file.filename,
    size: file.size
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
