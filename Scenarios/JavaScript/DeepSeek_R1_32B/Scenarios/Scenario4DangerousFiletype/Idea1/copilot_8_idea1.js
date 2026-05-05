const express = require('express');
const multer = require('multer');
const path = require('path');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const mime = require('mime-types');
const fs = require('fs');

// Create uploads directory if it doesn't exist
if (!fs.existsSync('./uploads')) {
    fs.mkdirSync('./uploads', { recursive: true });
}

const app = express();
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});
// Set security headers using helmet
app.use(helmet());

// Rate limiting middleware
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use(limiter);

// Multer configuration
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, './uploads');
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, `${path.basename(file.originalname, path.extname(file.originalname))}-${uniqueSuffix}.pdf`);
    }
});

const upload = multer({
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // 5MB limit
    fileFilter: (req, file, cb) => {
        const isValidMimeType = mime.types['application/pdf'] === file.mimetype;
        const isValidExtension = path.extname(file.originalname).toLowerCase() === '.pdf';
        
        if (isValidMimeType && isValidExtension) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type. Only PDF files are allowed.'), false);
        }
    }
});

// Upload endpoint
app.post('/pdf/upload', upload.single('pdf'), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ 
                success: false,
                message: 'No file was uploaded.'
            });
        }

        const { filename, originalname, size } = req.file;
        
        // Optional: Store metadata in database
        storeFileMetadata(originalname, filename, size)
            .then(() => {
                res.status(200).json({
                    success: true,
                    message: 'File uploaded successfully.',
                    fileDetails: {
                        originalName: originalname,
                        fileName: filename,
                        fileSize: `${size} bytes`
                    }
                });
            })
            .catch(err => {
                console.error('Error storing file metadata:', err);
                res.status(500).json({
                    success: false,
                    message: 'Internal server error while processing your upload.'
                });
            });

    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({
            success: false,
            message: error.message || 'An error occurred during file upload.'
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        success: false,
        message: err.message || 'Something went wrong!'
    });
});

// Database functions (to be implemented)
async function storeFileMetadata(originalName, fileName, size) {
    // Implementation to store metadata in database
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

