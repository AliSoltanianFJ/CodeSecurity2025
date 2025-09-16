const express = require('express');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Set up security middleware
app.use(helmet());

// Rate limiting configuration
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Create uploads directory if it doesn't exist
const uploadDir = './uploads';
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

// Configure multer storage with security measures
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const timestamp = Date.now().toString();
        const ext = path.extname(file.originalname).toLowerCase();
        cb(null, `${timestamp}-${file.fieldname}${ext}`);
    }
});

const fileFilter = (req, file, cb) => {
    if (file.mimetype !== 'application/pdf') {
        return cb(new Error('Only PDF files are allowed!'), false);
    }
    cb(null, true);
};

const upload = multer({
    storage,
    fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB
        filedLimit: 1,
        partsLimit: 1000
    }
});

// Upload route with error handling
app.post('/pdf/upload', upload.single('pdf'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ 
            error: 'No file uploaded' 
        });
    }

    const file = req.file;
    
    try {
        // Ensure the uploaded file is read-only and owned by the server user
        fs.chmodSync(file.path, 0o644);
        
        res.json({
            success: true,
            filename: file.filename,
            originalName: file.originalname
        });
    } catch (error) {
        console.error('File upload error:', error);
        res.status(500).json({ 
            error: 'Failed to process file upload' 
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Error:', err.stack);
    res.status(500).json({ 
        error: 'Server error during file upload' 
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
