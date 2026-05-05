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
    cb(null, './uploads'); // Store files in the ./uploads directory
  },
  filename: (req, file, cb) => {
    crypto.randomBytes(16, (err, raw) => {
      if (err) {
        return cb(err);
      }
      const sanitizedFilename = file.originalname.replace(/[^a-zA-Z0-9.]/g, "_");
      cb(null, raw.toString('hex') + path.extname(sanitizedFilename));
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
    fileSize: 5 * 1024 * 1024 // Limit file size to 5MB
  }
});

/**
 * save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res, next) => {
  // Input validation: Check if file exists
  if (!req.file) {
    return res.status(400).send('No file uploaded.');
  }

  // Sanitize file properties (filename, mimetype)
  const file = req.file;
  const sanitizedFilename = path.basename(file.filename); // Extract filename and sanitize
  const sanitizedMimetype = file.mimetype;

  // Further validation: Check file extension
  if (path.extname(sanitizedFilename).toLowerCase() !== '.pdf') {
    // Delete the invalid file
    fs.unlink(file.path, (err) => {
      if (err) {
        // Handle error (do not expose sensitive information)
        console.error('Error deleting invalid file:', err);
      }
      return res.status(400).send('Invalid file extension. Only PDF files are allowed.');
    });
    return;
  }

  // Check for directory traversal attempts in filename
  if (sanitizedFilename.includes("..")) {
    // Delete the invalid file
    fs.unlink(file.path, (err) => {
      if (err) {
        // Handle error (do not expose sensitive information)
        console.error('Error deleting potentially malicious file:', err);
      }
      return res.status(400).send('Invalid filename.');
    });
    return;
  }

  // Respond with success message
  res.status(200).send('File uploaded successfully.');
});

// Centralized error handling middleware
app.use((err, req, res, next) => {
  console.error("Global error handler: ", err.message); // Log the error on the server

  if (err instanceof multer.MulterError) {
    // A Multer error occurred when uploading.
    return res.status(400).send('Multer error: ' + err.message);
  } else if (err) {
    // Handle other errors
    return res.status(500).send('An unexpected error occurred.');
  }

  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});