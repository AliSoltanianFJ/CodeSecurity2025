const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

// Create uploads directory if it doesn't exist
if (!fs.existsSync("./uploads")) {
    fs.mkdirSync("./uploads", { recursive: true });
}

const app = express();

// Configure multer to handle file upload
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, "./uploads");
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '_' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({
    storage: storage,
    fileFilter: (req, file, cb) => {
        if (file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only PDF files are allowed!'), false);
        }
    }
});

// Set up MySQL database connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root', // replace with your username
    password: '',  // replace with your password
    database: 'uploads_db' // replace with your database name
});

pool.getConnection((err, connection) => {
    if (err) throw err;
    console.log('Connected to MySQL database');
    connection.release();
});

// Set security headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

// Handle file upload
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: "No PDF file was uploaded!" });
        }

        const { originalname, filename, size } = req.file;
        const uploadDate = Date.now();

        pool.query(
            'INSERT INTO files (original_name, file_path, size, upload_date) VALUES (?, ?, ?, ?)',
            [originalname, filename, size, uploadDate],
            function (err, result) {
                if (err) {
                    console.error('Error inserting into database:', err);
                    return res.status(500).json({ 
                        error: "An error occurred while saving file details to the database."
                    });
                }
                res.json({
                    message: 'PDF uploaded successfully',
                    fileDetails: {
                        originalName: originalname,
                        filePath: filename,
                        size: size + ' bytes',
                        uploadDate
                    }
                });
            }
        );
    } catch (error) {
        console.error('Error during file upload:', error);
        res.status(500).json({ 
            error: "An unexpected error occurred while processing your request."
        });
    }
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Something broke!' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

