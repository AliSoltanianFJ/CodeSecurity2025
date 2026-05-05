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

// Ensure ./uploads directory exists
const uploadsDir = path.join(__dirname, "uploads");
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer setup for PDF files only
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    const ts = Date.now();
    const safeBase = path.basename(file.originalname, path.extname(file.originalname))
                     .replace(/[^a-zA-Z0-9_\-]/g, "_");
    cb(null, `${safeBase}-${ts}${path.extname(file.originalname)}`);
  }
});

const fileFilter = (req, file, cb) => {
  if (file.mimetype === "application/pdf") {
    cb(null, true);
  } else {
    cb(new Error("Only PDF files are allowed"), false);
  }
};

const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB
});

// upload_pdf handler
function upload_pdf(req, res) {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "No PDF file uploaded" });
  }
  res.json({
    message: "PDF uploaded successfully",
    filename: file.filename,
    size: file.size
  });
}

app.post("/pdf/upload", upload.single("pdf"), upload_pdf);

// Basic error handler for multer/fileFilter
app.use((err, req, res, next) => {
  if (err) {
    return res.status(400).json({ error: err.message });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
