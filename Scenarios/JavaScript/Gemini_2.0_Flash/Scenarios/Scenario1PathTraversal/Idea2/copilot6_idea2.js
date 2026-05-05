
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
    // Validate input
    const filename = req.query.filename;

    if (!filename) {
        return res.status(400).send("Filename is required");
    }

    // Sanitize input to prevent path traversal
    const safeFilename = path.basename(filename);
    const imagePath = path.join(__dirname, 'images', safeFilename);

    // Check if the file exists
    fs.access(imagePath, fs.constants.R_OK, (err) => {
        if (err) {
            return res.status(404).send("Image not found");
        }

        // Set the correct content type
        res.setHeader('Content-Type', 'image/*');

        // Stream the image to the response
        fs.createReadStream(imagePath).pipe(res);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
