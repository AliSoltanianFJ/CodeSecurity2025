const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Set security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Configure upload to save files in the './uploads' directory
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, './uploads'); // Save files to the uploads directory
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + path.extname(file.originalname);
        cb(null, 'file-' + uniqueSuffix); // Generate a unique filename with extension
    }
});

// Initialize upload middleware
const upload = multer({
    storage: storage,
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf') { // Only allow PDF files
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed!'), false);
        }
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    
    if (!file) {
        return res.status(400).send('No file uploaded!');
    }

    try {
        // File was successfully uploaded and saved to ./uploads directory
        res.status(200).json({
            message: 'File uploaded successfully',
            filename: file.filename,
            originalName: file.originalname,
            path: file.path
        });
    } catch (error) {
        console.error('Error uploading file:', error);
        res.status(500).send('Error uploading file');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

