const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

// Configure multer for PDF uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, 'uploads');
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Generate unique filename using timestamp and random string
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9).toString();
    cb(null, `file-${uniqueSuffix}.pdf`);
  }
});

const fileFilter = (req, file, cb) => {
  // Only allow PDF files
  if (!file.mimetype.startsWith('application/pdf')) {
    return cb(new Error('Only PDF files are allowed!'), false);
  }
  cb(null, true);
};

// Configure upload with security settings
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: { // Prevent buffer overflow by limiting file size
    fileSize: 5 * 1024 * 1024 // Maximum file size of 5MB
  }
});

// Sanitize filename to prevent path traversal attacks
const sanitizeFilename = (filename) => {
  return path.basename(filename.replace(/\s+/g, '_'));
};

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        const file = req.file;
        if (!file) {
            return res.status(400).json({ error: 'No PDF file uploaded' });
        }

        // Validate and sanitize the filename
        const sanitizedFilename = sanitizeFilename(file.originalname);
        
        // Log upload success
        console.log(`File uploaded successfully`);
        
        res.json({
            filename: sanitizedFilename,
            path: file.path,
            size: file.size,
            mimetype: file.mimetype
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ 
            error: 'Internal server error during PDF upload' 
        });
    }
});

// Error handling middleware for multer uploads
app.use((err, req, res, next) => {
  console.error(err.stack);

  if (err.message.startsWith('Only PDF files are allowed!')) {
    return res.status(400).json({ error: 'Invalid file type. Only PDFs are accepted.' });
  }

  res.status(500).json({ 
      error: 'An unexpected error occurred while processing your request'
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

