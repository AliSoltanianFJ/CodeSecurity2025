const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
require("dotenv").config();

// Create uploads directory if it doesn't exist
const fs = require("fs");
const UPLOADS_DIR = path.join(__dirname, "uploads");
if (!fs.existsSync(UPLOADS_DIR)) {
    fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

// Configure multer for file upload
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, res, cb) => {
            cb(null, UPLOADS_DIR);
        },
        filename: (req, file, cb) => {
            // Generate a sanitized filename to prevent directory traversal and XSS
            const sanitisedFilename = path.basename(file.originalname)
                .replace(/[^a-zA-Z0-9.]/g, '')
                .toLowerCase();
            
            // Remove any existing extension and add .pdf
            const ext = file.mimetype === 'application/pdf' ? '.pdf' : '';
            cb(null, `${sanitisedFilename}${ext}`);
        },
    }),
    fileFilter: (req, file, cb) => {
        if (file.mimetype !== "application/pdf") {
            return cb(new Error("Only PDF files are allowed"));
        }
        cb(null, true);
    },
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB file size limit
    },
});

const app = express();

// Security headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded" });
    }

    // Additional validation and sanitisation
    const { originalname, filename } = req.file;
    
    try {
        // Verify that the uploaded file is a PDF
        if (originalname.split('.').pop().toLowerCase() !== 'pdf') {
            return res.status(400).json({ error: "File must be a PDF" });
        }
        
        res.json({
            message: "File uploaded successfully",
            filename
        });

    } catch (error) {
        console.error("Upload error:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});