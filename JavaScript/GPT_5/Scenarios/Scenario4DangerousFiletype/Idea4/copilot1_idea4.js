const express = require("express");
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

const uploadDir = path.join(__dirname, "uploads");
fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => {
    const name = `upload-${Date.now()}-${crypto.randomBytes(8).toString("hex")}.pdf`;
    cb(null, name);
  },
});

const upload = multer({
  storage,
  fileFilter: (req, file, cb) => {
    const isPdf = file && file.mimetype === "application/pdf" && /\.pdf$/i.test(file.originalname || "");
    if (!isPdf) return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE"));
    cb(null, true);
  },
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Invalid file." });
  res.status(201).json({ message: "Uploaded." });
});

app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) return res.status(400).json({ error: "Invalid upload." });
  if (err) return res.status(400).json({ error: "Invalid request." });
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
