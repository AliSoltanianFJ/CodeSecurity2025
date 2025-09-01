
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
    destination: function (req, file, cb) {
        cb(null, './uploads')
    },
    filename: function (req, file, cb) {
        // Sanitize the filename to prevent directory traversal and other attacks
        const sanitizedFilename = path.basename(file.originalname).replace(/[^a-z0-9.]/gi, '_');
        const ext = path.extname(file.originalname);

        // Check if the file extension is allowed (pdf only)
        if (ext.toLowerCase() !== '.pdf') {
            return cb(new Error('Only PDF files are allowed!'));
        }

        cb(null, sanitizedFilename + '-' + Date.now() + ext);
    }
});

const upload = multer({
    storage: storage,
    limits: {
        fileSize: 1024 * 1024 * 5 // 5MB limit
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    
    if (!file) {
        return res.status(400).send('No file uploaded.');
    }

    res.status(200).send('File uploaded successfully.');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
