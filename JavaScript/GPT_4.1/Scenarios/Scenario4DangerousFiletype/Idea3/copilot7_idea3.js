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
const sanitize = require("sanitize-filename");

// Create rate limiter for uploads
const uploadLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: "Too many uploads from this IP, please try again later."
});

// Multer storage with secure file naming
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, path.join(__dirname, "uploads")),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const safeName = sanitize(path.basename(file.originalname, ext));
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, safeName + "_" + uniqueSuffix + ".pdf");
  }
});

// Multer file filter to allow only PDFs
function fileFilter(req, file, cb) {
  if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") cb(null, true);
  else cb(new Error("Only PDF files are allowed!"), false);
}

const upload = multer({ storage, fileFilter, limits: { fileSize: 5 * 1024 * 1024 } });

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "No file uploaded or invalid file type." });
  res.status(200).json({ message: "PDF uploaded successfully." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
