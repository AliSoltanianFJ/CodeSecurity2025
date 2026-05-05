const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Security headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Configure multer storage
const upload = multer({
    storage: multer.diskStorage({
        destination: function (req, file, cb) {
            cb(null, './uploads/')
        },
        filename: function (req, file, cb) {
            // Sanitize the filename
            const sanitizedName = file.originalname.replace(/[^a-zA-Z0-9.-]/g, '_');
            cb(null, Date.now() + '-' + sanitizedName);
        }
    }),
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB max file size
    fileFilter: function (req, file, cb) {
        const allowedTypes = ['application/pdf'];
        if (allowedTypes.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type'), false);
        }
    }
});

// Input validation middleware
function validatePDF(req, res, next) {
    try {
        // Check if file exists and is a PDF
        if (!req.file || req.file.mimetype !== 'application/pdf') {
            throw new Error('Please upload a valid PDF file');
        }

        // Validate filename doesn't contain special characters
        const fileName = path.basename(req.file.originalname, '.pdf');
        if (!/^[a-zA-Z0-9.-]+$/.test(fileName)) {
            throw new Error('Invalid file name');
        }

        next();
    } catch (error) {
        console.error('Validation error:', error.message);
        res.status(400).json({ 
            success: false, 
            message: 'Invalid PDF file'
        });
    }
}

app.post("/pdf/upload", upload.single("pdf"), validatePDF, (req, res) => {
    try {
        const file = req.file;
        
        // Log the upload for auditing purposes
        console.log(`File uploaded successfully - ${file.filename}`);

        res.status(200).json({
            success: true,
            filename: file.filename,
            size: file.size,
            uploadDate: new Date().toISOString()
        });
    } catch (error) {
        console.error('Error uploading PDF:', error);
        res.status(500).json({
            success: false,
            message: 'Internal server error'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Secure server running on port ${PORT}`);
});

