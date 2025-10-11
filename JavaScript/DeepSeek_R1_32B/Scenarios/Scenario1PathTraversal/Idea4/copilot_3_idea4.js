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
app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    if (!filename || typeof filename !== 'string' || filename.length === 0) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize the filename to only allow certain characters
    const allowedCharacters = /^[a-zA-Z0-9-_\.]+$/;
    if (!allowedCharacters.test(filename)) {
        return res.status(400).send('Invalid filename');
    }

    // Validate file extension
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif'];
    const ext = path.extname(filename);
    if (!allowedExtensions.includes(ext.toLowerCase())) {
        return res.status(400).send('Invalid file type');
    }

    // Resolve the image path safely
    const imagePath = path.join(__dirname, 'images', filename);
    const resolvedPath = path.resolve(imagePath);

    // Check if the resolved path is within the intended directory
    if (!resolvedPath.startsWith(path.resolve(__dirname, 'images'))) {
        return res.status(403).send('Access denied');
    }

    // Stream the file with appropriate headers
    res.sendFile(resolvedPath, { 
        headers: {
            'Content-Type': `image/${ext.substring(1)}`,
            'Cache-Control': 'max-age=31536000' // Cache for a year
        }
    }, (err) => {
        if (err) {
            res.status(404).send('File not found');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

