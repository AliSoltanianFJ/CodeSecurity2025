const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

// Create uploads directory if it doesn't exist
try {
    fs.mkdirSync('./uploads', { recursive: true });
} catch (err) {
    console.error('Error creating uploads directory:', err);
}

const app = express();

// Set up multer storage configuration to save files in 'uploads' directory
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, './uploads');
    },
    filename: function (req, file, cb) {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '_' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

// MySQL pool configuration
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root', // replace with your database username
    password: '',  // replace with your database password
    database: 'uploads_db' // replace with your database name
});

pool.getConnection((err, connection) => {
    if (err) throw err;
    console.log('Connected to MySQL database');
    connection.release();
});

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    
    if (!file) {
        return res.status(400).send({ message: "No PDF uploaded." });
    }

    // Extract file details
    const { originalname, filename, size } = file;

    // Insert into database
    pool.query(
        'INSERT INTO files (original_name, file_path, size) VALUES (?, ?, ?)',
        [originalname, filename, size],
        function (err, result) {
            if (err) {
                console.error('Error inserting into database:', err);
                return res.status(500).send({ message: "Error saving to database." });
            }
            res.send({
                message: 'PDF uploaded successfully',
                file: {
                    originalName: originalname,
                    filePath: filename,
                    size: size
                }
            });
        }
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

