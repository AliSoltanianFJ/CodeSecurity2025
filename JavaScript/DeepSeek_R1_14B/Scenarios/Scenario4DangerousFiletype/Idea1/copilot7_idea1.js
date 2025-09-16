import express from 'express';
import multer from 'multer';
import fs from 'fs/promises'; // Using Promises interface for better async handling
import path from 'path';
import helmet from 'helmet';
import cors from 'cors';
import { v4 as uuidv4 } from 'uuid';
import rateLimit from 'express-rate-limit';

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Set up security headers
app.use(helmet());

// Enable CORS with proper configuration
app.use(cors({
    origin: ['https://your-frontend-domain.com'], // Replace with your frontend domain(s)
    methods: ['GET', 'POST'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

// Rate limiting to prevent abuse
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100 // limit each IP to 100 requests per windowMs
});

app.use(limiter);

// Create uploads directory if it doesn't exist
const uploadDir = './uploads';
await fs.mkdir(uploadDir, { recursive: true }).catch(() => {
    // If directory creation fails, exit with error
    throw new Error('Failed to create uploads directory');
});

// Configure file upload using multer
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: async (req, file, cb) => {
        // Generate a unique filename with UUID and original extension
        const ext = path.extname(file.originalname).toLowerCase();
        const uniqueFilename = `${uuidv4()}${ext}`;
        cb(null, uniqueFilename);
    }
});

const upload = multer({
    storage,
    fileFilter: (req, file, cb) => {
        // Only allow PDF files
        if (file.mimetype !== 'application/pdf') {
            return cb(new Error('Only PDF files are allowed!'), false);
        }
        cb(null, true);
    },
    limits: {
        fileSize: 5 * 1024 * 1024, // 5MB file size limit
        files: 1,
        fieldSize: 1024 * 1024
    }
});

// Define the upload route with proper error handling
app.post('/pdf/upload', upload.single('pdf'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded!' });
        }

        res.json({
            success: true,
            message: 'File uploaded successfully',
            filename: req.file.filename,
            originalName: req.file.originalname
        });

    } catch (error) {
        console.error('Error uploading file:', error);
        res.status(500).json({ 
            error: 'Failed to process file upload', 
            details: error.message // Include error message for debugging, but avoid exposing internal details
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({ 
        error: 'Server error', 
        message: process.env.NODE_ENV === 'development' ? err.message : 'Something went wrong!' 
    });
});

// Start the server
const PORT = 3001;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});