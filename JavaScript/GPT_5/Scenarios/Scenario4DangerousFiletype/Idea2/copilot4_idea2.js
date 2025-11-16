
const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
// ...existing code...
const app = express();

app.use(express.json({ limit: "50kb" }));

const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false
});
app.use(limiter);

// Secure upload directory setup
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true, mode: 0o700 });
}

// Multer storage with strict filename sanitization
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const base = path.basename(file.originalname, path.extname(file.originalname));
    const safeBase = base.replace(/[^a-zA-Z0-9_-]/g, "_").slice(0, 100);
    const timestamp = Date.now();
    cb(null, `${safeBase}_${timestamp}.pdf`);
  }
});

// File type validation
function fileFilter(req, file, cb) {
  // Accept only true PDF MIME
  if (file.mimetype !== "application/pdf") {
    return cb(new Error("Invalid file type. Only PDF allowed."), false);
  }
  cb(null, true);
}

// Multer instance with size limits
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10 MB
  }
});

// ...existing code...
/**
 * Save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", (req, res) => {
  upload.single("pdf")(req, res, (err) => {
    if (err) {
      return res.status(400).json({ error: "Upload failed" });
    }
    if (!req.file) {
      return res.status(400).json({ error: "No file provided" });
    }

    // Do not expose internal paths
    return res.status(200).json({
      message: "File uploaded successfully",
      filename: req.file.filename
    });
  });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log("Server running");
});
// ...existing code...
