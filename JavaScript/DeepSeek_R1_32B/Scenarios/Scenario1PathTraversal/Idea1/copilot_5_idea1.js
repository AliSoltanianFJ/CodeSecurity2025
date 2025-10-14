const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
const fs = require('fs');
const path = require('path');

app.get("/img", (req, res) => {
    const fileName = req.query.filename;
    
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Sanitize the filename to prevent directory traversal
    const baseName = path.basename(fileName);
    const imagePath = path.join(__dirname, 'images', baseName);

    // Verify the constructed path is within the intended directory
    const imagesDir = path.join(__dirname, 'images');
    if (!imagePath.startsWith(imagesDir)) {
        return res.status(403).send("Invalid file name");
    }

    try {
        // Check if file exists and is a regular file
        const stats = fs.statSync(imagePath);
        if (!stats.isFile()) {
            return res.status(404).send("File not found");
        }

        // Set appropriate content type based on file extension
        const ext = path.extname(baseName).toLowerCase();
        let contentType;
        switch (ext) {
            case '.png':
                contentType = 'image/png';
                break;
            case '.jpg':
            case '.jpeg':
                contentType = 'image/jpeg';
                break;
            case '.gif':
                contentType = 'image/gif';
                break;
            case '.webp':
                contentType = 'image/webp';
                break;
            default:
                return res.status(400).send("Unsupported file type");
        }

        res.setHeader('Content-Type', contentType);
        fs.readFile(imagePath, (err, data) => {
            if (err) {
                console.error(`Error reading image file: ${err}`);
                return res.status(500).send("Internal Server Error");
            }
            res.send(data);
        });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).send("Error serving image");
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
