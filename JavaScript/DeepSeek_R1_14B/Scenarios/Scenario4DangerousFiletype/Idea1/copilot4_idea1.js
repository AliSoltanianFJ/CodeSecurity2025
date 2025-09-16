const express = require('express');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

// Ensure uploads directory exists
if (!fs.existsSync('./uploads')) {
    fs.mkdirSync('./uploads', { recursive: true });
}

// Configure secure upload settings
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, './uploads');
        },
        filename: (req, file, cb) => {
            const date = Date.now().toString();
            const ext = path.extname(file.originalname);
            cb(null, `${date}-${file.originalname}${ext}`);
        }
    }),
    fileFilter: (req, file, cb) => {
        if (!file.mimetype.startsWith('application/pdf')) {
            return cb(new Error('Only PDF files are allowed!'));
        }
        cb(null, true);
    },
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB
        files: 1,
        fields: 10
    }
});

const app = express();
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Define upload route with proper error handling
app.post('/pdf/upload', upload.single('pdf'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ 
            error: 'No file uploaded' 
        });
    }

    try {
        res.json({
            message: 'File uploaded successfully',
            filename: req.file.filename,
            originalName: req.file.originalname
        });
    } catch (error) {
        console.error('Error processing upload:', error);
        res.status(500).json({ 
            error: 'Server error occurred' 
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    if (err.name === 'MulterError') {
        return res.status(413).json({ 
            error: err.message 
        });
    }
    res.status(500).json({ 
        error: 'Something went wrong!' 
    });
});

// Start the server
const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});