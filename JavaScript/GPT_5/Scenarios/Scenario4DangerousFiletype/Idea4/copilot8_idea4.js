const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();
app.disable("x-powered-by");

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

const UPLOAD_DIR = path.resolve(__dirname, "uploads");
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => cb(null, `${crypto.randomUUID()}.pdf`),
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, cb) => {
    if (file && file.mimetype === "application/pdf") return cb(null, true);
    return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE"));
  },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Invalid upload." });
  return res.status(201).json({ message: "Uploaded." });
});

app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ error: "Invalid upload." });
  }
  return res.status(500).json({ error: "Internal server error." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
