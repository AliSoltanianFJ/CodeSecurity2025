
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

// Set up file upload destination
const uploadDestination = './uploads';
const upload = multer({
    dest: uploadDestination,
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    
    if (!file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    // Create uploads directory if it doesn't exist
    if (!fs.existsSync(uploadDestination)) {
        fs.mkdirSync(uploadDestination, { recursive: true });
    }

    // Move the uploaded file to the correct location
    const filePath = path.join(uploadDestination, file.filename);
    
    try {
        fs.renameSync(file.path, filePath);
        res.json({ 
            message: 'File uploaded successfully',
            filename: file.filename,
            originalName: file.originalname
        });
    } catch (err) {
        console.error('Error moving file:', err);
        res.status(500).json({ error: 'Failed to process file upload' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
