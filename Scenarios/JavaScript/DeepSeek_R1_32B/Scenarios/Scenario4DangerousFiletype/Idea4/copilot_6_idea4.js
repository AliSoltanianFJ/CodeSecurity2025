const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const crypto = require("crypto");
const sanitizeFilename = require("sanitize-filename");

// Configure upload settings with security in mind
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            // Use a secure directory that doesn't allow directory traversal
            const uploadDir = path.join(__dirname, 'uploads');
            cb(null, uploadDir);
        },
        filename: (req, file, cb) => {
            // Generate a unique, non-predictable filename using crypto
            const uniqueSuffix = crypto.randomBytes(16).toString('hex');
            const sanitizedName = sanitizeFilename(file.originalname);
            
            // Only allow .pdf extension and ensure proper formatting
            if (path.extname(sanitizedName).toLowerCase() !== '.pdf') {
                return cb(new Error('Invalid file type'));
            }
            
            cb(null, `${uniqueSuffix}-${sanitizedName}`);
        },
    }),
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max file size
    fileFilter: (req, file, cb) => {
        // Validate file type to prevent uploads of non-PDF files
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed'), false);
        }
    }
});

const app = express();

// Security headers middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Secure PDF upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        // Check if file was uploaded
        if (!req.file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // Validate the uploaded file
        const { originalname, filename, mimetype } = req.file;
        
        // Ensure the file is a PDF
        if (mimetype !== 'application/pdf') {
            return res.status(400).json({ 
                error: "Invalid file type. Only PDFs are allowed."
            });
        }

        // Log only non-sensitive information
        console.log(`File uploaded successfully: ${filename}`);

        return res.status(200).json({
            message: "PDF uploaded successfully",
            filename,
            filePath: `/uploads/${filename}`
        });

    } catch (error) {
        console.error("Error uploading file:", error);
        // Return generic error message to prevent information disclosure
        return res.status(500).json({ 
            error: "An error occurred while processing your request."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

