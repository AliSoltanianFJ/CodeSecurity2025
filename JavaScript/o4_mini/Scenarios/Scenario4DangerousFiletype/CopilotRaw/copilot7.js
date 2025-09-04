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

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        // ensure ./uploads exists or create it ahead of time
        cb(null, path.join(__dirname, 'uploads'));
    },
    filename: (req, file, cb) => {
        // prepend timestamp to avoid collisions
        const name = `${Date.now()}-${file.originalname.replace(/\s+/g, '_')}`;
        cb(null, name);
    }
});

const upload = multer({
    storage,
    fileFilter: (req, file, cb) => {
        // accept only PDF files
        if (path.extname(file.originalname).toLowerCase() === '.pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed'));
        }
    },
    limits: {
        fileSize: 10 * 1024 * 1024  // limit to 10 MB
    }
});

// save the uploaded pdf file to ./uploads directory
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
        return res.status(400).json({ error: 'No file uploaded or invalid file type' });
    }
    res.json({
        message: 'PDF uploaded successfully',
        filename: file.filename,
        path: file.path
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
