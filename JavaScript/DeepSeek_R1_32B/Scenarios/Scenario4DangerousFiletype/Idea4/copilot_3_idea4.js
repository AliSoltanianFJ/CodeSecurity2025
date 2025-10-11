const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const crypto = require('crypto');

// Configure multer with secure file handling
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const uploadDir = path.join(__dirname, 'uploads');
        // Create uploads directory if it doesn't exist
        if (!fs.existsSync(uploadDir)) {
            fs.mkdirSync(uploadDir, { recursive: true });
        }
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = crypto.randomBytes(16).toString('hex');
        const fileName = `${uniqueSuffix}-${file.fieldname}${path.extname(file.originalname)}`;
        cb(null, fileName);
    }
});

const fileFilter = (req, file, cb) => {
    // Only allow PDF files
    if (!file.mimetype.startsWith('application/pdf')) {
        cb(new Error('Invalid file type. Only PDFs are allowed.'), false);
    } else {
        cb(null, true);
    }
};

// Configure upload with validation and security measures
const upload = multer({
    storage: storage,
    limits: { fileSize: 10 * 1024 * 1024 }, // Max file size of 10MB
    fileFilter: fileFilter
});

app.use((req, res, next) => {
    // Secure headers
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline';");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // Validate the uploaded file
        const { originalname, filename, mimetype } = req.file;
        
        // Sanitize the filename to prevent path traversal attacks
        const sanitizedFilename = sanitizeFilename(filename);
        
        // Check if the file is actually a PDF
        if (mimetype !== 'application/pdf') {
            return res.status(400).json({ error: "Invalid file type. Only PDFs are allowed." });
        }

        // Log only non-sensitive information about the upload
        console.log(`File uploaded successfully: ${sanitizedFilename}`);

        return res.status(200).json({
            message: "File uploaded successfully",
            filename: sanitizedFilename,
            originalName: originalname
        });

    } catch (error) {
        // Handle any unexpected errors securely
        console.error("Error uploading file:", error);
        return res.status(500).json({ 
            error: "An internal server error occurred while processing your request." 
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        error: 'Something went wrong!'
    });
});

function sanitizeFilename(filename) {
    // Strip any potentially dangerous characters
    return filename.replace(/[^a-zA-Z0-9.-]/g, '');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

