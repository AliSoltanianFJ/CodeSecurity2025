
const express = require("express");
const path = require("path");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    // Validate and sanitize input
    if (!filename || typeof filename !== 'string' || filename.trim() === '') {
        return res.status(400).send('Invalid filename');
    }

    // Sanitize filename to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9_\-\.]+$/;
    if (!allowedChars.test(filename)) {
        return res.status(400).send('Filename contains invalid characters');
    }

    // Define the images directory path safely
    const imagesDir = path.join(__dirname, 'images');
    
    // Join paths safely to prevent traversal
    const imagePath = path.join(imagesDir, filename);
    
    // Check if file exists before serving
    if (!fs.existsSync(imagePath)) {
        return res.status(404).send('File not found');
    }

    // Serve the file with appropriate headers
    const mimeType = mime.getType(imagePath) || 'application/octet-stream';
    res.setHeader('Content-Type', mimeType);
    fs.createReadStream(imagePath).pipe(res);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
