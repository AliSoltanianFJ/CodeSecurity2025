const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
require('dotenv').config();

// Create uploads directory if it doesn't exist
const fs = require('fs');
const uploadDir = './uploads';
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure multer storage
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        // Sanitize the original filename to prevent path traversal and injection attacks
        const sanitisedFilename = path.basename(file.originalname)
            .replace(/[^a-zA-Z0-9_.]/g, '_')
            .replace(/\.\./g, ''); // Replace any characters that aren't allowed
        
        cb(null, `${sanitisedFilename}${path.extname(file.originalname)}`);
    }
});

const upload = multer({
    storage: storage,
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB file size limit
        files: 1
    },
    fileFilter: (req, file, cb) => {
      // Validate file type
      if (!file.mimetype.includes('pdf')) {
        cb(new Error('Only PDF files are allowed'), false);
        return;
      }
      cb(null, true);
    },
});

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Secure file upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded" });
    }

    // Validate the uploaded file
    if (req.file.mimetype !== 'application/pdf') {
        return res.status(400).json({ error: "Only PDF files are allowed" });
    }

    const fileName = path.basename(req.file.path);
    
    // Send response
    res.json({ message: `File ${fileName} uploaded successfully` });
});

// Error handling middleware
app.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // Handle file upload errors
        return res.status(500).json({ error: err.message });
    }
    next(err);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});