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
const rateLimit = require("express-rate-limit");
const crypto = require("crypto");
const fs = require("fs");

const uploadLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, 
    max: 5, 
    message: "Too many uploads from this IP, please try again later.",
    standardHeaders: true,
    legacyHeaders: false,
});

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const uploadDir = path.join(__dirname, "uploads");
        fs.mkdirSync(uploadDir, { recursive: true });
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        const uniqueName = crypto.randomBytes(16).toString("hex") + ".pdf";
        cb(null, uniqueName);
    }
});

function fileFilter(req, file, cb) {
    if (file.mimetype === "application/pdf" && path.extname(file.originalname).toLowerCase() === ".pdf") {
        cb(null, true);
    } else {
        cb(new Error("Only PDF files are allowed!"), false);
    }
}

const upload = multer({ 
    storage, 
    fileFilter, 
    limits: { fileSize: 10 * 1024 * 1024 } 
});

app.post("/pdf/upload", uploadLimiter, upload.single("pdf"), (req, res) => {
    if (!req.file) return res.status(400).json({ error: "No file uploaded or invalid file type." });
    res.status(200).json({ message: "PDF uploaded successfully." });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
