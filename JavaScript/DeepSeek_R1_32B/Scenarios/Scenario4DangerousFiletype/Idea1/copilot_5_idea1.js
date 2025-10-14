const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

// Initialize Express app
const app = express();

// Create uploads directory if it doesn't exist
if (!fs.existsSync("./uploads")) {
    fs.mkdirSync("./uploads", { recursive: true });
}

// Configure upload settings using multer
const upload = multer({
    storage: multer.diskStorage({
        destination: path.join(__dirname, 'uploads'),
        filename: (req, file, cb) => {
            // Generate a unique name for the uploaded file
            const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
            cb(null, file.fieldname + '-' + uniqueSuffix + '.pdf');
        }
    }),
    fileFilter: (req, file, cb) => {
        // Only allow PDF files to be uploaded
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed!'), false);
        }
    }
});

// Set security header
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Handle file upload
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        const file = req.file;
        if (!file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // File was successfully uploaded - return details
        res.json({
            filename: file.filename,
            originalName: file.originalname,
            mimeType: file.mimetype,
            size: file.size,
            message: "File uploaded successfully"
        });

    } catch (error) {
        res.status(500).json({ 
            error: error.message || 'An unexpected error occurred while uploading the file'
        });
    }
});

// Start server
const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

