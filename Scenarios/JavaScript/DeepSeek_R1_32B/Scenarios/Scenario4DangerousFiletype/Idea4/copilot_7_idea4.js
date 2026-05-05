const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require('fs');
const { v4: uuidv4 } = require('uuid');

// Configure multer storage with security measures
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      const uploadsDir = './uploads';
      if (!fs.existsSync(uploadsDir)) {
        fs.mkdirSync(uploadsDir, { recursive: true });
      }
      cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
      // Sanitize the original filename
      const sanitizedName = path.basename(file.originalname)
        .replace(/[^a-zA-Z0-9_.]/g, '_')
        .toLowerCase();
      
      // Add UUID to prevent filename collisions and directory traversal
      cb(null, `${uuidv4()}-${sanitizedName}`);
    },
  }),
  fileFilter: (req, file, cb) => {
    // Only allow PDF files
    if (file.mimetype === 'application/pdf') {
      cb(null, true);
    } else {
      cb(new Error('Only PDF files are allowed'), false);
    }
  },
  limits: { fileSize: 5 * 1024 * 1024 }, // Max file size of 5MB
});

const app = express();

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        const file = req.file;
        
        if (!file) {
            return res.status(400).json({ error: "No PDF file uploaded" });
        }

        // Validate file details
        if (!file.originalname || !file.mimetype || !file.size) {
            return res.status(400).json({ error: "Invalid file parameters" });
        }

        // Check for upload errors
        if (req.fileValidationError) {
            return res.status(400).json({ error: req.fileValidationError.message });
        }

        res.json({
            status: 'success',
            uploadedFile: file.filename,
            message: 'PDF file uploaded successfully'
        });

    } catch (error) {
        console.error('Error uploading PDF:', error);
        res.status(500).json({ 
            error: "Internal server error while processing the upload"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

