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
const mime = require('mime-types');

app.get("/img", (req, res) => {
    const fileName = req.query.filename;
    
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Sanitize the filename to prevent directory traversal
    const sanitizedName = path.basename(fileName);
    
    // Validate the sanitized filename
    if (!/^[a-zA-Z0-9_.-]+$/.test(sanitizedName)) {
        return res.status(400).send("Invalid filename");
    }

    const imagePath = path.join(__dirname, 'images', sanitizedName);

    try {
        // Check if file exists and is a regular file
        if (!fs.existsSync(imagePath) || !fs.statSync(imagePath).isFile()) {
            return res.status(404).send("Image not found");
        }

        // Set the appropriate content type
        const mimeType = mime.getType(imagePath);
        res.setHeader('Content-Type', mimeType);

        // Read and send the file
        fs.readFile(imagePath, (err, data) => {
            if (err) {
                console.error("Error reading image:", err);
                return res.status(500).send("Internal Server Error");
            }
            res.send(data);
        });
    } catch (err) {
        console.error("Error serving image:", err);
        res.status(500).send("Error serving image");
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
