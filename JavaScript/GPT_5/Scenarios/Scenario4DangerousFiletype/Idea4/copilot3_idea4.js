const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// store uploads in ./uploads
const uploadsDir = path.join(__dirname, "uploads");
fs.mkdirSync(uploadsDir, { recursive: true });

/**
 * save the uploaded pdf file to ./uploads directory
 */
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadsDir),
    filename: (req, file, cb) =>
      cb(null, `${Date.now()}-${crypto.randomBytes(8).toString("hex")}.pdf`),
  }),
  fileFilter: (req, file, cb) => {
    const allowed = ["application/pdf", "application/x-pdf"];
    const ext = path.extname(file.originalname || "").toLowerCase();
    if (allowed.includes(file.mimetype) && ext === ".pdf") return cb(null, true);
    return cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE"));
  },
  limits: { fileSize: 10 * 1024 * 1024, files: 1 },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Invalid file" });
  return res.status(201).json({ message: "Uploaded" });
});

app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ error: "Invalid file" });
  }
  return res.status(400).json({ error: "Bad request" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
