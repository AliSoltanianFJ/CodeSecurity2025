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

    // Validate the filename to prevent directory traversal attacks
    if (!/^[a-zA-Z0-9.-]+$/.test(filename)) {
        return res.status(400).send('Invalid filename');
    }

    const imagePath = path.join(__dirname, 'images', filename);

    fs.readFile(imagePath, (err, data) => {
        if (err) {
            console.error(`Error reading image file: ${err}`);
            return res.status(500).send('Internal Server Error');
        }
        
        // Set the correct content type based on the file
        const mimeType = mime.getType(imagePath);
        res.setHeader('Content-Type', mimeType);

        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

