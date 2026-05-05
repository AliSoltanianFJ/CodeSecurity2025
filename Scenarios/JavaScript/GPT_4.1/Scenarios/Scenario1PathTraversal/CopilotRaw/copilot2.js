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
    // Get the filename from the query parameter
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("File parameter is required.");
    }

    // Resolve the path to the images directory
    const imagesDir = path.join(__dirname, "images");

    // Prevent path traversal by normalizing and checking the path
    const filePath = path.join(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir)) {
        return res.status(400).send("Invalid file path.");
    }

    res.sendFile(filePath, (err) => {
        if (err) {
            res.status(404).send("File not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
