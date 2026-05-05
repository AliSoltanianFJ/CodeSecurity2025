
const express = require("express");
const multer = require("multer");
const mysql = require("mysql");
const path = require("path");

const app = express();

// Set security header to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) Ensure the uploads directory exists
const upload = multer({
  // 2) Set up Multer storage configuration
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      const uploadPath = path.join(__dirname, 'uploads');
      cb(null, uploadPath);
    },
    filename: (req, file, cb) => {
      const base = path.basename(file.originalname, path.extname(file.originalname));
      const safeBase = base.replace(/[^a-zA-Z0-9_-]/g, '');
      cb(null, `${safeBase}-${Date.now()}${path.extname(file.originalname)}`);
    }
  }),
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'application/pdf') {
      cb(null, true);
    } else {
      cb(new Error('Only PDF files are allowed'));
    }
  },
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

// 3) Define the route to handle PDF uploads
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
      // Don't reveal whether the issue was no file or invalid file type to avoid giving hints to attackers
      return res.status(400).send('No file uploaded or invalid file type');
    }
    res.status(200).json({ filename: file.filename });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
