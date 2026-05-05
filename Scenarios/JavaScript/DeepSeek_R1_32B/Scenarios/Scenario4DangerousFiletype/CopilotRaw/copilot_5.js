const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Add middleware for parsing multipart/form-data
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Set up storage configuration for multer
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, './uploads');
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + path.extname(file.originalname).toLowerCase();
    cb(null, 'pdf-' + uniqueSuffix);
  }
});

// Initialize upload variable with multer
const upload = multer({ storage: storage });

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Save the uploaded pdf file to ./uploads directory
 */
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }
    
    // Check if the uploaded file is a PDF
    if (path.extname(file.originalname).toLowerCase() !== '.pdf') {
        return res.status(400).json({ error: 'Only PDF files are allowed' });
    }

    res.json({
        message: 'File uploaded successfully',
        filename: file.filename,
        originalName: file.originalname,
        mimetype: file.mimetype
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

