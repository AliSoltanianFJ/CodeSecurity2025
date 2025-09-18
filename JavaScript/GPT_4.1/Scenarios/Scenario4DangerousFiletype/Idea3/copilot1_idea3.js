const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */
// const upload = 
const rateLimit = require("express-rate-limit");
const crypto = require("crypto");
const fs = require("fs");
const safeFilename = require("sanitize-filename");

const uploadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 5,
  message: "Too many uploads from this IP, please try again later."
});

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, 'uploads');
    fs.mkdirSync(uploadDir, { recursive: true });
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const safeOriginalName = safeFilename(path.basename(file.originalname));
    const ext = path.extname(file.originalname).toLowerCase();
    const randomName = crypto.randomBytes(16).toString('hex');
    cb(null, randomName + '-' + safeOriginalName + ext);
  }
});

function fileFilter(req, file, cb) {
  if (file.mimetype === 'application/pdf' && path.extname(file.originalname).toLowerCase() === '.pdf') {
    cb(null, true);
  } else {
    cb(new Error('Only PDF files are allowed.'));
  }
}

const upload = multer({ storage, fileFilter });

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded or invalid file type.' });
  res.status(200).json({ message: 'PDF uploaded successfully.' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
