const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */
// const upload = 
// ...existing code...

/**
 * Secure Multer configuration for PDF uploads.
 * - Only allows .pdf files (MIME type and extension).
 * - Limits file size to 5MB.
 * - Stores files in ./uploads with a sanitized filename.
 * - Prevents directory traversal and overwriting.
 */
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            cb(null, path.resolve(__dirname, 'uploads'));
        },
        filename: (req, file, cb) => {
            // Sanitize filename: remove dangerous characters, use timestamp
            const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\-.]/g, '');
            const ext = path.extname(safeName).toLowerCase();
            const base = path.basename(safeName, ext);
            cb(null, `${base}_${Date.now()}${ext}`);
        }
    }),
    fileFilter: (req, file, cb) => {
        // Only accept PDF files by MIME type and extension
        const allowedMime = 'application/pdf';
        const allowedExt = '.pdf';
        if (
            file.mimetype === allowedMime &&
            path.extname(file.originalname).toLowerCase() === allowedExt
        ) {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed.'));
        }
    },
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

// ...existing code...
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    // Validate that a file was uploaded
    if (!req.file) {
        // Do not leak internal error details
        return res.status(400).json({ error: "No PDF file uploaded or file type invalid." });
    }

    // Additional validation: check file path is within uploads directory
    const uploadsDir = path.resolve(__dirname, 'uploads');
    const filePath = path.resolve(req.file.path);
    if (!filePath.startsWith(uploadsDir)) {
        // Defensive check against directory traversal
        return res.status(400).json({ error: "Invalid file path." });
    }

    // At this point, file is securely saved
    // Do not return file path or sensitive info to client
    res.status(201).json({ message: "PDF uploaded successfully." });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
