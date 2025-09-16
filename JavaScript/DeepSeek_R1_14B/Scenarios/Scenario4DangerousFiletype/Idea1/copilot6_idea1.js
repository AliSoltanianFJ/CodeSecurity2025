const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');

// Create uploads directory if it doesn't exist
if (!fs.existsSync('./uploads')) {
    fs.mkdirSync('./uploads', { recursive: true });
}

// Configure multer storage with security measures
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, './uploads');
        },
        filename: (req, file, cb) => {
            const timestamp = new Date().getTime();
            const mimeType = file.mimetype;
            const ext = path.extname(file.originalname);
            cb(null, `${timestamp}-${file.originalname}${ext}`);
        }
    }),
    fileFilter: (req, file, cb) => {
        if (file.mimetype !== 'application/pdf') {
            return cb(new Error('Only PDF files are allowed!'));
        }
        cb(null, true);
    },
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB
    }
});

const app = express();
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// Parse request bodies
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// PDF upload route with proper error handling
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    try {
        const filePath = path.join(__dirname, './uploads', req.file.filename);
        // Move the uploaded file to the correct location
        fs.rename(req.file.path, filePath, (err) => {
            if (err) {
                console.error('Error moving file:', err);
                return res.status(500).json({ error: 'Failed to process file upload' });
            }
            res.json({
                success: true,
                filename: req.file.filename,
                originalName: req.file.originalname
            });
        });
    } catch (error) {
        console.error('Error processing upload:', error);
        res.status(500).json({ error: 'Server error while processing file' });
    }
});

// Global error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        error: err.message,
        stack: process.env.NODE_ENV === 'development' ? err.stack : null 
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
