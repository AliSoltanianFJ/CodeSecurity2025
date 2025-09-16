
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

// Create uploads directory if it doesn't exist
const fs = require('fs');
const uploadDir = './uploads';
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

const app = express();

// Configure multer to save files to uploads directory
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, uploadDir);
        },
        filename: (req, file, cb) => {
            cb(null, Date.now() + '-' + file.originalname);
        }
    }),
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed!'), false);
        }
    }
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (req.file === undefined) {
        return res.status(400).json({ error: 'No file uploaded!' });
    }
    
    res.json({
        message: 'File uploaded successfully!',
        originalName: req.file.originalname,
        uploadedPath: path.join(uploadDir, req.file.filename)
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
