const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const crypto = require('crypto');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
  next();
});

// Configure storage for uploaded files
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, './uploads');
  },
  filename: (req, file, cb) => {
    crypto.randomBytes(16, (err, raw) => {
      if (err) {
        return cb(err);
      }
      cb(null, raw.toString('hex') + path.extname(file.originalname));
    });
  }
});

// File filter to only allow PDF files
const fileFilter = (req, file, cb) => {
  if (file.mimetype === 'application/pdf') {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Only PDF files are allowed.'), false);
  }
};

// Initialize multer with the configuration
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  }
});

/**
 * save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).send('No file uploaded.');
  }

  // Sanitize file path to prevent path traversal
  const filePath = path.resolve('./uploads', path.basename(req.file.filename));

  // Verify that the file exists where we expect it
  fs.access(filePath, fs.constants.F_OK, (err) => {
    if (err) {
      return res.status(500).send('File not found after upload.');
    }

    // Respond with success message
    res.status(200).send('File uploaded successfully.');
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    // A Multer error occurred when uploading.
    return res.status(400).send(err.message);
  } else if (err) {
    // An unknown error occurred.
    return res.status(500).send('An unexpected error occurred.');
  }

  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});