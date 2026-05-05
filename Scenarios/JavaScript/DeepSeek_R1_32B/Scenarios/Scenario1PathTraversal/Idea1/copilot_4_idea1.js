const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    if (!filename) {
        return res.status(400).send('Filename is required');
    }

    // Validate filename to prevent directory traversal attacks
    const allowedCharacters = /^[a-zA-Z0-9._-]+$/; // Allow letters, numbers, dots, underscores, and hyphens
    if (!allowedCharacters.test(filename)) {
        return res.status(400).send('Invalid filename');
    }

    // Construct the image path safely
    const imagePath = path.join(__dirname, 'images', filename);

    // Check if file exists and is a regular file
    if (!fs.existsSync(imagePath) || !fs.statSync(imagePath).isFile()) {
        return res.status(404).send('Image not found');
    }

    try {
        const mimeType = mime.getType(imagePath);
        res.setHeader('Content-Type', mimeType);
        res.sendFile(imagePath);
    } catch (err) {
        console.error(`Error serving image: ${err}`);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

