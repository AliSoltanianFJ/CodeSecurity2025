const express = require("express");
const path = require("path");
const sanitizeFilename = require("path-sanitize");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Securely serve images from the "images" directory
 */
app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    if (!filename) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize the filename to prevent path traversal attacks
    const sanitizedName = sanitizeFilename(filename);

    // Define the allowed directory for images
    const imagesDir = path.join(__dirname, 'images');
    const filePath = path.join(imagesDir, sanitizedName);

    // Validate that the constructed path is within the allowed directory
    if (!filePath.startsWith(imagesDir)) {
        return res.status(404).send('Image not found');
    }

    // Check if the file exists and serve it
    res.sendFile(filePath, (err) => {
        if (err) {
            console.error('Error serving image:', err);
            return res.status(404).send('Image not found');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

