const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs"); // Added to check if directory exists

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create uploads directory if it doesn't exist
if (!fs.existsSync('./uploads')) {
    fs.mkdirSync('./uploads', { recursive: true });
}

/**
 * Save the uploaded pdf file to ./uploads directory
 */
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, './uploads');
        },
        filename: (req, file, cb) => {
            // Use timestamp + original filename to avoid duplicates
            const uniqueSuffix = Date.now() + '_' + path.extname(file.originalname);
            cb(null, file.fieldname + uniqueSuffix);
        }
    }),
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed!'), false);
        }
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded!' });
        }
        
        // File was uploaded successfully
        const uploadedFile = req.file;
        console.log('File uploaded:', uploadedFile.path);

        return res.json({
            filename: uploadedFile.filename,
            path: uploadedFile.path,
            size: uploadedFile.size + ' bytes'
        });

    } catch (error) {
        console.error('Error uploading file:', error);
        return res.status(500).json({ 
            error: error.message || 'An error occurred while uploading the file.'
        });
    }
});

// Error handling middleware for multer
app.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // Handle Multer-specific errors
        return res.status(400).json({ 
            error: 'An error occurred during file upload.'
        });
    }
    // Pass other errors to next middleware or default error handler
    next(err);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

