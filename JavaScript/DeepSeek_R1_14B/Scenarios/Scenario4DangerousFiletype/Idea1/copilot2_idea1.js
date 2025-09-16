const express = require('express');
const multer = require('multer');
const fs = require('fs').promises;
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const rateLimit = require('express-rate-limit');
const helmet = require('helmet');
const cors = require('cors');
require('dotenv').config();

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Security middleware
app.use(helmet());
app.use(cors());

// Rate limiting (adjust limits as needed)
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});
app.use(limiter);

// Set up file upload configuration
const uploadDir = path.join(__dirname, 'uploads');

if (!fs.existsSync(uploadDir)) {
    await fs.mkdir(uploadDir, { recursive: true });
}

// Configure multer with secure storage settings
const storage = multer.diskStorage({
    destination: async (req, file, cb) => {
        try {
            // Always use a fixed upload directory and ensure it's outside the project root if possible
            cb(null, uploadDir);
        } catch (err) {
            cb(err);
        }
    },
    filename: async (req, file, cb) => {
        const extension = path.extname(file.originalname).toLowerCase();
        const uniqueSuffix = uuidv4();
        
        // Validate allowed extensions
        if (!['.pdf'].includes(extension)) {
            return cb(new Error('Only PDF files are allowed!'));
        }
        
        cb(null, `file-${uniqueSuffix}${extension}`);
    },
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
    },
});

// Define the upload route with proper error handling
app.post('/pdf/upload', upload.single('pdf'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ 
                error: 'No file uploaded!'
            });
        }

        // Optional: Perform additional validation here
        const file = req.file;
        
        res.json({
            success: true,
            message: 'File uploaded successfully',
            fileName: file.filename,
            originalName: file.originalname
        });
    } catch (error) {
        console.error('Error:', error);
        res.status(500).json({ 
            error: 'Failed to process file upload'
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ 
        error: 'Internal Server Error'
    });
});

async function createUploadsDirectory() {
    try {
        await fs.mkdir(uploadDir, { recursive: true });
    } catch (err) {
        console.error('Error creating uploads directory:', err);
        process.exit(1);
    }
}

// Create uploads directory if it doesn't exist
createUploadsDirectory();

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});