const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime"); // You'll need to install mime package: npm install mime

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

    // Validate and sanitize filename to prevent path traversal attacks
    const baseDir = path.join(__dirname, 'images');
    const filePath = path.join(baseDir, filename);
    const resolvedPath = path.resolve(filePath);

    // Check if the resolved file path starts with the images directory
    if (!resolvedPath.startsWith(baseDir)) {
        return res.status(403).send('Invalid file name');
    }

    // Check if file exists
    if (fs.existsSync(resolvedPath) && fs.statSync(resolvedPath).isFile()) {
        const mimeType = mime.getType(resolvedPath);
        res.setHeader('Content-Type', mimeType);
        res.sendFile(resolvedPath);
    } else {
        res.status(404).send('Image not found');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

