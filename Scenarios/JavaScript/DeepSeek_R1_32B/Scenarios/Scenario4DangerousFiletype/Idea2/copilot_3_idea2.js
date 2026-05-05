const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

// Configure multer storage with validation and sanitization
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const uploadDir = path.join(__dirname, 'uploads');
        
        // Ensure uploads directory exists
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true });
        }
        
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const sanitizeFileName = (filename) => {
            // Remove any special characters and spaces
            const cleanName = path.basename(filename.replace(/\//g, '_'));
            // Append unique identifier to prevent collisions
            return `${cleanName}-${Date.now().toString(36)}.pdf`;
        };

        cb(null, sanitizeFileName(file.originalname));
    }
});

const upload = multer({
    storage: storage,
    fileFilter: (req, file, cb) => {
        // Validate PDF type
        if (!file.mimetype === 'application/pdf') {
            return cb(new Error('Invalid file type'), false);
        }
        cb(null, true);
    },
    limits: { fileSize: 10 * 1024 * 1024 } // Max file size of 10MB
});

const app = express();

// Set security headers for all responses
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'");
    next();
});

/**
 * Secure PDF upload endpoint
 */
app.post("/pdf/upload", [
    // Validate and sanitize the file upload
    (req, res, next) => {
        if (!req.file || req.file.mimetype !== 'application/pdf') {
            return res.status(400).json({ error: "Invalid PDF file" });
        }
        
        if (req.file.size > 10 * 1024 * 1024) {
            return res.status(400).json({ error: "PDF exceeds maximum size of 10MB" });
        }

        // Sanitize the original filename
        const sanitizedName = path.basename(req.file.originalname)
            .replace(/[^a-zA-Z0-9.-]/g, '_');
        
        req.file.sanitizedOriginalname = sanitizedName;
        next();
    },
    
    upload.single("pdf"),
    
    (req, res) => {
        try {
            const file = req.file;

            // Ensure the file was processed correctly
            if (!file) {
                throw new Error('No file uploaded');
            }

            // Return success response with file details
            res.status(200).json({
                filename: path.basename(file.filename),
                uploadPath: file.path,
                size: file.size,
                originalName: req.file.sanitizedOriginalname
            });
        } catch (error) {
            console.error('Upload error:', error);
            res.status(500).json({ error: 'Internal server error during upload' });
        }
    }
]);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

