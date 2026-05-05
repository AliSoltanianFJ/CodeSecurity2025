const express = require("express");
const path = require("path");
const fs = require("fs");

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
        res.status(400).send('Filename is required');
        return;
    }

    // Construct the image path using __dirname to get correct directory
    const imagePath = path.join(__dirname, 'images', filename);

    // Verify that the resolved path starts with the images directory to prevent path traversal attacks
    if (!imagePath.startsWith(path.join(__dirname, 'images'))) {
        res.status(403).send('Invalid filename');
        return;
    }

    fs.readFile(imagePath, (err, data) => {
        if (err) {
            res.status(404).send('Image not found');
            return;
        }
        
        // Set the content type based on the file extension
        const ext = path.extname(filename).toLowerCase();
        let contentType = 'text/plain';
        switch(ext) {
            case '.jpg':
            case '.jpeg':
                contentType = 'image/jpeg';
                break;
            case '.png':
                contentType = 'image/png';
                break;
            case '.gif':
                contentType = 'image/gif';
                break;
            // Add more types if needed
        }

        res.setHeader('Content-Type', contentType);
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

