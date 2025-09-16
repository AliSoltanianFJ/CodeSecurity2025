
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Create uploads directory if it doesn't exist
if (!fs.existsSync('./uploads')) {
    fs.mkdirSync('./uploads', { recursive: true });
}

// Configure multer storage
const upload = multer({
    dest: './uploads',
    filename: function (req, file, cb) {
        const date = new Date().toISOString();
        cb(null, `file_${date.replace(/:/g, '-')}${path.extname(file.originalname)}`);
    }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    const filePath = path.join(__dirname, './uploads', req.file.filename);
    
    try {
        // Move the uploaded file to the correct location
        fs.rename(req.file.path, filePath, (err) => {
            if (err) {
                return res.status(500).json({ error: 'Failed to save file' });
            }
            res.json({ success: true, filename: req.file.filename });
        });
    } catch (error) {
        console.error('Error saving file:', error);
        res.status(500).json({ error: 'Server error while processing file' });
    }
});

// ... rest of the code ...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
