const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

// Storage configuration for uploaded files
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    // Restrict directory traversal and ensure safe upload path
    const uploadDir = './uploads';
    if (!path.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Sanitize filename to prevent directory traversal and malicious characters
    const allowedChars = /^[a-zA-Z0-9._-]+$/;
    let sanitizedName = file.originalname.replace(/[^\w.-]/g, '');
    if (!allowedChars.test(sanimizedName)) {
      cb(new Error('Invalid filename'), null);
      return;
    }
    // Add timestamp to avoid overwriting files
    const timestamp = new Date().getTime();
    cb(null, `${sanitizedName}_${timestamp}`);
  },
});

// File upload configuration with security constraints
const upload = multer({
  storage: storage,
  fileFilter: (req, file, cb) => {
    // Validate file type
    if (!file.mimetype.includes('pdf')) {
      cb(new Error('Only PDF files are allowed'), false);
      return;
    }
    cb(null, true);
  },
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB file size limit
  },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const errors = req.validationErrors();
    
    if (errors || !req.file) {
        return res.status(400).json({
            success: false,
            message: 'Invalid file or upload failed'
        });
    }

    // Move the uploaded file to a secure location
    const file = req.file;
    const destPath = path.join(__dirname, 'uploads', file.filename);

    // Additional security check for uploaded files
    fs.stat(destPath, (err, stats) => {
        if (err || !stats.isFile()) {
            return res.status(500).json({
                success: false,
                message: 'Failed to save the file'
            });
        }

        res.status(201).json({
            success: true,
            message: 'File uploaded successfully',
            fileName: file.filename,
            size: stats.size,
            previewUrl: `http://${req.headers.host}/uploads/${file.filename}`
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});