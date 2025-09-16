const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const crypto = require('crypto');
const helmet = require('helmet');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Security middleware
app.use(helmet());

// Set up upload directory if it doesn't exist
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer storage with security measures
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
        const allowedExtensions = ['.pdf'];
        const ext = path.extname(file.originalname).toLowerCase();
        
        if (!allowedExtensions.includes(ext)) {
            cb(new Error('Only PDF files are allowed!'), false);
            return;
        }
        
        // Generate a unique filename using UUID and original extension
        const uniqueId = crypto.randomUUID();
        cb(null, `${uniqueId}${ext}`);
    },
});

const fileFilter = (req, file, cb) => {
    if (file.mimetype !== 'application/pdf') {
        cb(new Error('Only PDF files are allowed!'), false);
    } else {
        cb(null, true);
    }
};

// Set up limits
const FILE_SIZE_LIMIT = 5 * 1024 * 1024; // 5MB
const multerUpload = multer({
    storage,
    fileFilter,
    limits: {
        fileSize: FILE_SIZE_LIMIT,
    },
});

app.use(express.json());

app.post('/pdf/upload', multerUpload.single('pdf'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded!' });
    }

    try {
        // Optional: Remove the temporary upload file after processing
        fs.unlinkSync(req.file.path);
        
        res.json({
            success: true,
            filename: req.file.filename,
            originalName: req.file.originalname,
        });
    } catch (error) {
        console.error('Error handling uploaded file:', error);
        res.status(500).json({ 
            error: 'Failed to process file upload',
            message: error.message 
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);

    if (err.name === 'MulterError') {
        return res.status(400).json({ 
            error: err.message,
            code: 'INVALID_FILE'
        });
    }

    res.status(500).json({
        error: 'Server error',
        message: 'An unexpected error occurred while processing your request.'
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});