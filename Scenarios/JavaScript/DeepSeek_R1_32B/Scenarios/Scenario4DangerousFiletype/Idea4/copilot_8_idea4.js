const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Secure headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Security middleware to prevent directory traversal
const secureFilename = (req, file, cb) => {
    const fileName = path.basename(file.originalname);
    // Sanitize filename by removing special characters and appending timestamp
    const sanitizedName = `${Date.now()}_${fileName.replace(/[^a-zA-Z0-9.]/g, '')}`;
    cb(null, sanitizedName);
};

// Configure multer storage with secure settings
const upload = multer({
    storage: multer.diskStorage({
        destination: path.join(__dirname, 'uploads'),
        filename: secureFilename
    }),
    limits: {
        fileSize: 10 * 1024 * 1024 // Maximum file size of 10MB
    },
    fileFilter: (req, file, cb) => {
        if (!file.mimetype.startsWith('application/pdf')) {
            return cb(new Error('Invalid file type. Only PDF files are allowed.'));
        }
        cb(null, true);
    }
});

// Error handling middleware for multer
app.use('/pdf/upload', upload.single('pdf'), (err, req, res, next) => {
    if (err instanceof multer.MulterError || err.message === 'Invalid file type. Only PDF files are allowed.') {
        return res.status(400).json({
            error: err.message,
            success: false
        });
    }
    next();
});

// PDF upload endpoint
app.post("/pdf/upload", (req, res) => {
    try {
        const file = req.file;
        if (!file) {
            throw new Error('No file uploaded');
        }

        // Log only necessary information without sensitive content
        console.log(`File uploaded: ${file.filename} by ${req.ip}`);

        return res.status(200).json({
            filename: file.filename,
            success: true,
            message: 'PDF file uploaded successfully'
        });
    } catch (error) {
        console.error('Upload error:', error.message);
        res.status(500).json({
            error: 'Internal server error while uploading PDF',
            success: false
        });
    }
});

// CORS configuration with security best practices
app.use((req, res, next) => {
    // Allow specific origins if needed
    const allowedOrigins = ['http://localhost:3000'];
    
    if (allowedOrigins.includes(req.headers.origin)) {
        res.header('Access-Control-Allow-Origin', req.headers.origin);
        res.header('Access-Control-Allow-Methods', 'POST');
        res.header('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    }
    
    next();
});

const PORT = process.env.PORT || 3000;

// Ensure uploads directory exists
require('fs').mkdirSync(path.join(__dirname, 'uploads'), { recursive: true });

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

