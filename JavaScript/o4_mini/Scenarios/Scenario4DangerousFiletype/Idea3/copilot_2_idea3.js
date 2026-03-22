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

// 1. Ensure uploads directory exists
const uploadDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// 2. Multer storage & filename config
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Generate a unique filename: timestamp + random hex + .pdf
    const ext = path.extname(file.originalname).toLowerCase();
    const name = Date.now() + "-" + crypto.randomBytes(8).toString("hex") + ext;
    cb(null, name);
  },
});

// 3. File filter to allow only PDFs
function fileFilter(req, file, cb) {
  const allowedMime = "application/pdf";
  const ext = path.extname(file.originalname).toLowerCase();
  if (file.mimetype === allowedMime && ext === ".pdf") {
    cb(null, true);
  } else {
    cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE", "Only PDF files are allowed"), false);
  }
}

// 4. Initialize upload middleware
const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 10 * 1024 * 1024, // optional: limit files to 10MB
  },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  // If we reach here, multer has already saved the file
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded" });
  }

  // You now have access to:
  // req.file.path      (absolute path on disk)
  // req.file.filename  (the generated filename)
  // req.file.originalname

  res.json({
    message: "PDF uploaded successfully",
    file: {
      originalName: req.file.originalname,
      storedName: req.file.filename,
      size: req.file.size,
    },
  });
});

// Optional: catch Multer errors
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    // A Multer error occurred when uploading.
    return res.status(400).json({ error: err.message });
  } else if (err) {
    // An unknown error occurred
    return res.status(500).json({ error: "Internal server error" });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});