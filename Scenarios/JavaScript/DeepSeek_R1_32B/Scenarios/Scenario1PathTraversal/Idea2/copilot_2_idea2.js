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

    // Validate input to prevent path traversal and ensure it's a valid filename
    if (!filename || typeof filename !== 'string' || !/^[a-zA-Z0-9_.\-]+$/.test(filename)) {
        return res.status(400).send('Invalid filename');
    }

    // Sanitize the filename to prevent directory traversal
    const imagePath = path.join(__dirname, 'images', filename);

    // Check if the file exists and is within the images directory
    try {
        const stats = fs.statSync(imagePath);
        if (stats.isFile()) {
            res.setHeader('X-Content-Type-Options', 'nosniff');
            res.sendFile(imagePath);
        } else {
            res.status(404).send('File not found');
        }
    } catch (err) {
        // Handle any errors, such as file not found
        console.error('Error accessing image:', err);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

