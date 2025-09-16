const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const helmet = require('helmet');

const app = express();

// Set up uploads directory
const uploadDir = './uploads';
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure secure headers
app.use(helmet());

// Configure file upload with security measures
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, uploadDir);
        },
        filename: (req, file, cb) => {
            const sanitizedName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_.]/g, '');
            cb(null, `${Date.now()}-${sanitizedName}${path.extname(file.originalname)}`);
        }
    }),
    fileFilter: (req, file, cb) => {
        if (file.mimetype !== 'application/pdf') {
            return cb(new Error('Only PDF files are allowed!'), false);
        }
        cb(null, true);
    },
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB
        files: 1
    }
});

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Protect against large uploads and DOS attacks
app.use('/pdf/upload', (req, res, next) => {
    const sizeLimit = '5mb';
    req.checkBodySize({ limit: sizeLimit });
    next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded!' });
    }

    try {
        res.json({
            message: 'File uploaded successfully',
            filename: req.file.filename,
            originalName: req.file.originalname
        });
    } catch (error) {
        console.error('Error processing upload:', error);
        res.status(500).json({ error: 'Server error during file upload' });
    }
});

// Global error handler
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        error: err.message,
        details: process.env.NODE_ENV === 'development' ? err : undefined 
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
