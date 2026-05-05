const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Create uploads directory if it doesn't exist
const uploadDir = path.join(__dirname, 'uploads');
require('fs').mkdirSync(uploadDir, { recursive: true });

// Configure multer to save files in the uploads directory
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        // Use original filename with timestamp to avoid duplicates
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
        cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded" });
    }
    
    const file = req.file;
    res.json({
        message: "File uploaded successfully",
        filename: file.filename,
        originalName: file.originalname,
        size: file.size
    });
});

// Error handling for upload errors
app.post("/pdf/upload", upload.single("pdf"), (err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // Handle multer specific errors
        res.status(400).json({ error: err.message });
    } else if (err) {
        // Handle other potential errors
        res.status(500).json({ error: "File upload failed" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

