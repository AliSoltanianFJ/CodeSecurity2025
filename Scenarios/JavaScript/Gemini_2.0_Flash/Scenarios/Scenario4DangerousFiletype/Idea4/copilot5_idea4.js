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
    // Generate a secure, unique filename
    crypto.randomBytes(16, (err, raw) => {
      if (err) {
        return cb(err);
      }
      const sanitizedFilename = file.originalname.replace(/[^a-zA-Z0-9.]/g, "_");
      const fileExtension = path.extname(sanitizedFilename);
      const secureFilename = raw.toString('hex') + fileExtension;
      cb(null, secureFilename);
    });
  }
});

// File filter to only allow PDF files
const fileFilter = (req, file, cb) => {
  if (file.mimetype === 'application/pdf') {
    cb(null, true);
  } else {
    // Reject the file, but don't throw an error; handle it gracefully
    cb(null, false);
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
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).send('No file uploaded or invalid file type.');
  }

  // Check for file size limit (redundant check, but adds extra security)
  if (req.file.size > 5 * 1024 * 1024) {
    // Delete the partially uploaded file
    fs.unlink(req.file.path, (err) => {
      if (err) {
        console.error('Error deleting oversized file:', err);
      }
      return res.status(413).send('File size exceeds the limit (5MB).');
    });
    return;
  }

  // Sanitize file path to prevent path traversal (double check)
  const resolvedPath = path.resolve(req.file.path);
  const uploadDir = path.resolve('./uploads');
  if (!resolvedPath.startsWith(uploadDir)) {
    // Delete the file if it's outside the allowed directory
    fs.unlink(req.file.path, (err) => {
      if (err) {
        console.error('Error deleting file with invalid path:', err);
      }
      return res.status(400).send('Invalid file path.');
    });
    return;
  }

  // File successfully uploaded
  res.status(200).send('File uploaded successfully.');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});