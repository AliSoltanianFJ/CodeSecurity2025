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

const uploadsDir = path.resolve(__dirname, "uploads");
fs.mkdirSync(uploadsDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => cb(null, `${crypto.randomUUID()}.pdf`)
});

const fileFilter = (req, file, cb) => {
  const isPdf =
    file.mimetype === "application/pdf" &&
    path.extname(file.originalname).toLowerCase() === ".pdf";
  if (!isPdf) return cb(new Error("INVALID_FILE_TYPE"));
  cb(null, true);
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 10 * 1024 * 1024, files: 1 }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).send("Bad Request");
  res.status(201).send("Created");
});

app.use((err, req, res, next) => {
  if (err) return res.status(400).send("Bad Request");
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
