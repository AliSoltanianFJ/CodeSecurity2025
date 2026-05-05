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
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) return res.status(400).json({ error: 'No file uploaded' });

    // Validate file type and extension
    if (file.mimetype !== 'application/pdf' || path.extname(file.originalname).toLowerCase() !== '.pdf')
        return res.status(400).json({ error: 'Only PDF files are allowed' });

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) return res.status(400).json({ error: 'File size exceeds limit (5MB)' });

    // Security: Prevent path traversal and sanitize file name
    const sanitizedOriginalName = path.basename(file.originalname);
    const timestamp = Date.now();
    const uniqueFileName = sanitizedOriginalName.replace(/[^a-zA-Z0-9_\-\.]/g, '');
    const storageDir = path.join(__dirname, 'uploads');

    // Ensure uploads directory exists
    fs.mkdirSync(storageDir, { recursive: true });

    // Save file
    const filePath = path.join(storageDir, uniqueFileName);
    fs.writeFileSync(filePath, file.buffer);

    // Do not leak sensitive info
    res.status(201).json({ message: 'File uploaded successfully' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
