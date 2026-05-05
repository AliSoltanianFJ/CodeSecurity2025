const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Set up middleware to prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Configure upload settings: allow only PDF files and limit file size (if needed)
const upload = multer({
    dest: path.join(__dirname, 'uploads'),
    fileFilter: (req, file, cb) => {
        const allowedTypes = ['application/pdf'];
        if (allowedTypes.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type. Only PDF is allowed.'), false);
        }
    },
    limits: { fileSize: 5000000 }, // Optional: limit file size to 5MB
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
        return res.status(400).json({ error: "No file uploaded." });
    }

    // Sanitize filename to prevent directory traversal attacks
    const safeFileName = path.basename(file.originalname);
    const uploadPath = path.join(__dirname, 'uploads', safeFileName);

    // Rename the uploaded file to the sanitized name
    try {
        require('fs').renameSync(file.path, uploadPath);
        return res.json({ message: "File uploaded successfully.", filename: safeFileName });
    } catch (err) {
        console.error("Error processing uploaded file:", err);
        return res.status(500).json({ error: "Internal server error." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});