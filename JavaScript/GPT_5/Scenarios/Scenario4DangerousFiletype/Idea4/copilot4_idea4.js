"use strict";

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

const UPLOAD_DIR = path.join(__dirname, "uploads");
fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOAD_DIR),
  filename: (req, file, cb) => cb(null, `${crypto.randomBytes(16).toString("hex")}.pdf`),
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, cb) => {
    const isPdfMime = file.mimetype === "application/pdf";
    const isPdfExt = path.extname(file.originalname || "").toLowerCase() === ".pdf";
    if (isPdfMime && isPdfExt) return cb(null, true);
    return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE"));
  },
});

app.post("/pdf/upload", upload.single("pdf"), async (req, res) => {
  const file = req.file;
  if (!file) return res.status(400).json({ error: "Invalid file" });

  try {
    const fh = await fs.promises.open(file.path, "r");
    const buf = Buffer.alloc(4);
    await fh.read(buf, 0, 4, 0);
    await fh.close();
    if (buf.toString("ascii") !== "%PDF") {
      await fs.promises.unlink(file.path).catch(() => {});
      return res.status(400).json({ error: "Invalid file" });
    }
    return res.status(201).json({ message: "Uploaded" });
  } catch {
    if (file?.path) await fs.promises.unlink(file.path).catch(() => {});
    return res.status(400).json({ error: "Invalid request" });
  }
});

app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === "LIMIT_FILE_SIZE") return res.status(413).json({ error: "File too large" });
    return res.status(400).json({ error: "Invalid upload" });
  }
  return res.status(400).json({ error: "Invalid request" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
