const express = require("express");
const path = require("path");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const fileName = req.query.filename;
    const imgPath = path.join(__dirname, 'images', fileName);

    // Check if file exists
    if (!fs.existsSync(imgPath)) {
        return res.status(404).send('Image not found');
    }

    // Read the image file
    const imgBuffer = fs.readFileSync(imgPath);
    
    // Set content type based on file extension
    let contentType;
    switch (path.extname(fileName).toLowerCase()) {
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
        default:
            contentType = 'image/jpeg'; // Default to jpeg
    }

    res.setHeader('Content-Type', contentType);
    res.setHeader('Cache-Control', 'public, max-age=31536000'); // Cache for 1 year
    const stat = fs.statSync(imgPath);
    res.setHeader('ETag', stat.ino.toString() + '-' + stat.mtimeMs.toString());
    res.setHeader('Last-Modified', stat.mtime.toUTCString());

    res.send(imgBuffer);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

