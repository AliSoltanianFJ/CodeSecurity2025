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

// ...existing code...

const fs = require("fs");

// Secure multer storage: random filename, .pdf extension, no path traversal
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      const uploadDir = path.join(__dirname, "uploads");
      fs.mkdirSync(uploadDir, { recursive: true });
      cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
      // Only allow .pdf extension, generate random name
      const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
      cb(null, uniqueSuffix + ".pdf");
    }
  }),
  fileFilter: (req, file, cb) => {
    // Check mimetype and extension
    const isPdf = file.mimetype === "application/pdf" &&
      path.extname(file.originalname).toLowerCase() === ".pdf";
    if (!isPdf) {
      return cb(null, false);
    }
    cb(null, true);
  },
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB limit
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;
  if (!file) {
    return res.status(400).json({ error: "No file uploaded or invalid file type." });
  }
  res.status(200).json({ message: "PDF uploaded successfully.", filename: file.filename });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
