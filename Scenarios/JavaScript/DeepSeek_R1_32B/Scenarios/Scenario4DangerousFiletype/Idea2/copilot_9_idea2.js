const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Add Content-Security-Policy headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'");
    next();
});

// Configure multer storage with security measures
const upload = multer({
    dest: path.join(__dirname, 'uploads'),
    limits: {
        fileSize: 5 * 1024 * 1024 // Max file size of 5MB
    },
    fileFilter(req, file, cb) {
        // Validate file type is PDF
        const allowedTypes = ['application/pdf'];
        if (allowedTypes.includes(file.mimetype)) {
            // Sanitize filename to prevent path traversal attacks
            const safeFilename = path.basename(file.originalname.replace(/[^a-zA-Z0-9.-]/g, '_'));
            cb(null, true);
        } else {
            cb(new Error('Invalid file type'), false);
        }
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        const file = req.file;
        
        if (!file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // Validate file type again as a secondary check
        if (file.mimetype !== 'application/pdf') {
            return res.status(400).json({ error: "Invalid file type. Only PDFs are allowed." });
        }

        // Check for valid filename characters
        const safeFilename = path.basename(file.originalname.replace(/[^a-zA-Z0-9.-]/g, '_'));
        if (!safeFilename) {
            return res.status(400).json({ error: "Invalid file name" });
        }

        // Move the uploaded file to a secure location
        upload.single("pdf")(req, res, (err) => {
            if (err) {
                console.error('Error moving uploaded file:', err);
                return res.status(500).json({ error: "Internal server error while processing file" });
            }

            // After validation and sanitization, we can safely process the file
            res.json({
                success: true,
                filename: safeFilename,
                size: file.size,
                uploadDate: new Date().toISOString()
            });
        });

    } catch (error) {
        console.error('Error processing PDF upload:', error);
        res.status(500).json({ error: "Internal server error" });
    }
});

// Add security headers middleware
app.use((req, res, next) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

