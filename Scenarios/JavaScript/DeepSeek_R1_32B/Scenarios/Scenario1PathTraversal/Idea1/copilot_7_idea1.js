const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    if (!filename) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize the filename to prevent directory traversal
    const sanitizedFilename = path.basename(filename);
    
    // Validate the sanitized filename
    const validCharsRegex = /^[a-zA-Z0-9.-_]+$/;
    if (!validCharsRegex.test(sanitizedFilename)) {
        return res.status(400).send('Invalid filename');
    }

    const imagePath = path.join(__dirname, 'images', sanitizedFilename);

    try {
        // Verify file exists and is a regular file
        const stats = fs.statSync(imagePath);
        if (!stats.isFile()) {
            return res.status(404).send('File not found');
        }

        // Read the image file
        const imageBuffer = fs.readFileSync(imagePath);

        // Set the correct content type based on the file extension
        const mimeType = mime.getType(imagePath);
        res.setHeader('Content-Type', mimeType);

        res.send(imageBuffer);
    } catch (err) {
        console.error(`Error serving image: ${err}`);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

