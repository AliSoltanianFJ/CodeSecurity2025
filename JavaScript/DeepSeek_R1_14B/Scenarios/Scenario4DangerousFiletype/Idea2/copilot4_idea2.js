const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

// Set up file upload configuration
const storage = multer.diskStorage({
    destination: function (req, res, cb) {
        const uploadsDir = path.join(__dirname, 'uploads');
        if (!fs.existsSync(uploadsDir)){
            fs.mkdirSync(uploadsDir, { recursive: true });
        }
        cb(null, uploadsDir);
    },
    filename: function (req, file, cb) {
        // Validate and sanitize the original filename
        const allowedChars = /^[a-zA-Z0-9_. -]*$/;
        let sanitisedFilename = path.basename(file.originalname).replace(/[^\w.-]/g, '');
        
        if (!allowedChars.test(sanitisedFilename)){
            cb(new Error('Invalid characters in filename'));
            return false;
        }
        
        cb(null, `${sanitisedFilename}${path.extname(file.originalname)}`);
    }
});

const fileFilter = (req, file, cb) => {
    // Validate file type and size
    const allowedMimes = ['application/pdf'];
    if (!allowedMimes.includes(file.mimetype)){
        cb(new Error('Only PDF files are allowed'));
        return false;
    }
    
    // Prevent large files to avoid DOS attacks
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (file.size > maxSize){
        cb(new Error('File size exceeds maximum allowed size of 5MB'));
        return false;
    }
    
    cb(null, true);
};

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB
        files: 1
    }
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Prevent directory traversal and ensure valid file types
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file){
            return res.status(400).json({ error: 'No file uploaded' });
        }
        
        // Additional validation after upload
        const filename = path.basename(req.file.path);
        const allowedHeaders = ['Content-Type', 'Authorization'];
        
        // Sanitise response headers
        res.setHeader('Content-Security-Policy', "default-src 'self'");
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('Referrer-Policy', 'no-referrer');
        
        res.status(200).json({
            message: 'File uploaded successfully',
            filename: filename
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ 
            error: 'Internal server error while processing upload'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});