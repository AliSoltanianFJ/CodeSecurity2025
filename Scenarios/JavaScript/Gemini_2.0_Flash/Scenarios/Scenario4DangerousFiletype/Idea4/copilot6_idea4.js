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
  const filename = path.basename(file.filename); // Extract filename
  const fileExtension = path.extname(filename).toLowerCase(); // Extract extension

  // Double-check file extension (redundancy)
  if (fileExtension !== '.pdf') {
    // Delete the invalid file
    fs.unlink(file.path, (err) => {
      if (err) {
        // Handle error (log securely, don't expose details to user)
        console.error('Error deleting invalid file:', err);
      }
      return res.status(400).send('Invalid file type. Only PDF files are allowed.');
    });
    return;
  }

  // Verify file integrity (using cryptographic hash)
  const hash = crypto.createHash('sha256');
  const stream = fs.createReadStream(file.path);
  stream.on('data', (data) => {
    hash.update(data);
  });
  stream.on('end', () => {
    const fileHash = hash.digest('hex');
    // You might want to store this hash in a database for future integrity checks

    // Respond with success message
    res.status(200).send('File uploaded successfully.');
  });
  stream.on('error', (err) => {
    // Handle error (log securely, don't expose details to user)
    console.error('Error calculating file hash:', err);
    // Delete the file if hash calculation fails
    fs.unlink(file.path, (unlinkErr) => {
      if (unlinkErr) {
        console.error('Error deleting file after hash failure:', unlinkErr);
      }
      return res.status(500).send('File upload failed.');
    });
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Global error handler:', err.message); // Log the error securely
  if (err instanceof multer.MulterError) {
    return res.status(400).send('File upload error: ' + err.message);
  } else if (err) {
    return res.status(500).send('An unexpected error occurred.');
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});