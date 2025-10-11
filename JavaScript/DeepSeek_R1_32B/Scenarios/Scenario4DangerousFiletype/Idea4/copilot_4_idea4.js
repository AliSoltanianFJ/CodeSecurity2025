const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const { v4: uuidv4 } = require('uuid');

// Create uploads directory if it doesn't exist
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir);
}

// Configure multer storage options
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        // Generate a unique filename with UUID and original extension
        const originalName = path.basename(file.originalname, path.extname(file.originalname));
        const sanitizedName = originalName.replace(/[^a-zA-Z0-9]/g, '_'); // Remove special characters
        const extension = path.extname(file.originalname).toLowerCase();
        
        cb(null, `${sanitizedName}-${uuidv4()}${extension}`);
    }
});

// Validate file type and size
const fileFilter = (req, file, cb) => {
    if (file.mimetype === 'application/pdf' && file.originalname.toLowerCase().endsWith('.pdf')) {
        cb(null, true);
    } else {
        cb(new Error('Invalid PDF file'), false);
    }
};

// Set up multer upload configuration
const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB maximum file size
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: "No PDF file uploaded" });
        }

        const { originalname, filename, mimetype, size } = req.file;
        
        // Additional validation
        if (mimetype !== 'application/pdf' || !originalname.toLowerCase().endsWith('.pdf')) {
            fs.unlinkSync(path.join(uploadDir, filename)); // Clean up invalid file
            return res.status(400).json({ error: "Invalid PDF file" });
        }

        // Log minimal information without sensitive data
        console.log(`PDF uploaded successfully - ${filename}`);

        res.status(200).json({
            success: true,
            filename: filename,
            originalName: originalname,
            size: size
        });

    } catch (error) {
        console.error("Error uploading PDF:", error.message);
        
        // Clean up any partially uploaded file
        if (req.file && req.file.filename) {
            const filePath = path.join(uploadDir, req.file.filename);
            if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
            }
        }

        res.status(500).json({
            error: "Internal server error while uploading PDF"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

