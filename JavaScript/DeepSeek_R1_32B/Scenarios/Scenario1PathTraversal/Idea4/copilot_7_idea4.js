const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime-types");

const app = express();

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Securely return images from the "images" folder based on a GET parameter.
 */
app.get("/img", (req, res) => {
    const fileName = req.query.filename;
    
    // Input validation and sanitization
    if (!fileName || typeof fileName !== 'string' || !/^[a-zA-Z0-9._-]+$/.test(fileName)) {
        return res.status(400).send('Invalid filename');
    }

    // Define the allowed directory
    const imagesDir = path.join(__dirname, 'images');
    
    // Sanitize the file name and construct the full path
    const filePath = path.join(imagesDir, fileName);
    
    // Check if the requested file exists within the allowed directory
    if (!fs.existsSync(filePath)) {
        return res.status(404).send('Image not found');
    }

    // Verify the file is an image (prevent serving other file types)
    const fileType = mime.getType(filePath);
    if (!fileType || !fileType.startsWith('image/')) {
        return res.status(403).send('Invalid file type');
    }

    // Set security headers
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Disposition', `inline; filename="${path.basename(filePath)}"`);

    // Stream the image file
    fs.createReadStream(filePath)
        .on('error', () => {
            res.status(500).send('Internal Server Error');
        })
        .pipe(res);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

