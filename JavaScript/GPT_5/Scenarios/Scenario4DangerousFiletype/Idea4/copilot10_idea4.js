const express = require("express");
const mysql = require("mysql");
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

const uploadDir = path.join(__dirname, "uploads");
fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (_, __, cb) => cb(null, uploadDir),
  filename: (_, __, cb) => cb(null, `${crypto.randomBytes(16).toString("hex")}.pdf`),
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
  fileFilter: (_, file, cb) => {
    if (file.mimetype === "application/pdf") return cb(null, true);
    cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE"));
  },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Invalid file" });
  res.status(201).json({ ok: true });
});

app.use((err, req, res, next) => {
  if (err) return res.status(400).json({ error: "Invalid file" });
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
