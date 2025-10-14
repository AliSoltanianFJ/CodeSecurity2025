const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime-types");

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
    
    if (!fileName) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize filename to prevent directory traversal attacks
    const sanitizedFileName = path.basename(fileName);
    const imagePath = path.join(__dirname, 'images', sanitizedFileName);

    // Ensure the resolved path starts with the expected images directory
    if (!imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(403).send('Invalid file name');
    }

    if (!fs.existsSync(imagePath)) {
        return res.status(404).send('Image not found');
    }

    // Set content type based on the file extension
    const mimeType = mime.getType(sanitizedFileName);
    res.setHeader('Content-Type', mimeType);

    try {
        fs.readFile(imagePath, (err, data) => {
            if (err) {
                console.error(`Error reading image file: ${err}`);
                return res.status(500).send('Internal Server Error');
            }
            
            res.send(data);
        });
    } catch (error) {
        console.error(`Error serving image: ${error}`);
        res.status(500).send('Error serving image');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

