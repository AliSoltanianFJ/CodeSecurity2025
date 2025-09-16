const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const helmet = require('helmet');

// Set up the uploads directory on application start
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true, mode: 0o755 }); // Use proper permissions
}

// Configure security headers
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.use(helmet());

// Limit the size of uploaded files (adjust as needed)
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// File upload configuration with security measures
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        // Always return the uploads directory
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        // Sanitize the filename to prevent path traversal and malicious names
        const allowedChars = /^[a-zA-Z0-9_.\-]+$/;
        let sanitizedFilename = file.originalname.replace(/[^\w.-]/g, '_');
        
        // Check if a file with the same name exists
        const uploadPath = path.join(uploadDir, sanitizedFilename);
        let attempt = 1;
        
        // Generate unique filename to avoid overwrites
        do {
            if (attempt > 1) {
                sanitizedFilename += `_${attempt}`;
            }
            uploadPath = path.join(uploadDir, sanitizedFilename);
            attempt++;
            
            try {
                if (!fs.existsSync(uploadPath)) {
                    break;
                }
            } catch (err) {
                // If file doesn't exist or access denied
                if (err.code !== 'ENOENT') {
                    cb(new Error('Failed to generate unique filename'), null);
                    return;
                }
            }
        } while (true);

        cb(null, sanitizedFilename);
    },
});

const fileFilter = (req, file, cb) => {
    // Only allow PDF files
    if (!file.mimetype.startsWith('application/pdf')) {
        cb(new Error('Only PDF files are allowed!'), false);
        return;
    }
    
    cb(null, true);
};

// Configure multer with security settings
const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB limit
        files: 1,
    },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        // Success response
        res.json({
            success: true,
            filename: req.file.filename,
            originalName: req.file.originalname,
        });
    } catch (error) {
        console.error('File upload failed:', error);
        
        if (error.code === 'LIMIT_FILE_SIZE') {
            return res.status(413).json({ error: 'File size exceeds limit' });
        }

        if (error.message.includes('is not allowed')) {
            return res.status(405).json({ error: 'Method not allowed or invalid format' });
        }

        // Return a generic server error to avoid exposing internal details
        return res.status(500).json({ error: 'Server error while processing file upload' });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Error occurred:', err);
    
    if (err.name === 'MulterError') {
        if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(413).json({ error: 'File size exceeds limit' });
        }
        if (err.code === 'UNEXPECTED_FILE_TYPE') {
            return res.status(405).json({ error: 'Only PDF files are allowed' });
        }
    }

    // Return a generic 500 error to avoid exposing internal details
    return res.status(500).json({ error: 'Internal server error' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});