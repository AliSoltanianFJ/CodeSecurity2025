const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const crypto = require("crypto");

const app = express();

// Harden HTTP headers
app.use((req, res, next) => {
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    next();
});

// Resolve and lock down uploads directory
const uploadDir = path.resolve(__dirname, "./uploads");
try {
    fs.mkdirSync(uploadDir, { recursive: true, mode: 0o700 });
} catch (err) {
    console.error("Unable to create uploads directory:", err);
    process.exit(1);
}

// Configure multer for secure PDF uploads
const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => {
            // Always write under our fixed, safe directory
            cb(null, uploadDir);
        },
        filename: (req, file, cb) => {
            const ext = path.extname(file.originalname).toLowerCase();
            // Only allow .pdf extension after checking mimetype
            const filename = crypto.randomUUID() + ext;
            cb(null, filename);
        }
    }),
    limits: {
        fileSize: 10 * 1024 * 1024 // 10 MB
    },
    fileFilter: (req, file, cb) => {
        // Whitelist PDF MIME type
        if (file.mimetype === "application/pdf") {
            cb(null, true);
        } else {
            cb(new multer.MulterError("LIMIT_UNEXPECTED_FILE", "Only PDF files are allowed"));
        }
    }
});

// Upload endpoint
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        // No file or filtered out
        return res.status(400).json({ error: "No valid PDF uploaded." });
    }
    // Success
    res.status(200).json({
        message: "File uploaded successfully.",
        filename: req.file.filename
    });
});

// Global error handler for multer‐specific errors
app.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        // Multer error codes documented here: https://github.com/expressjs/multer#errors
        return res.status(400).json({ error: err.message });
    }
    next(err);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});