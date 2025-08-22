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
    // Validate and sanitize the filename input
    const fileName = req.query.file;
    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(fileName) || // allow only safe characters
        fileName.length > 100 // prevent buffer overflow
    ) {
        return res.status(400).send("Invalid file name.");
    }

    // Prevent path traversal by resolving and checking the path
    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir)) {
        return res.status(400).send("Invalid file path.");
    }

    // Set safe headers
    res.setHeader("Content-Type", "image/jpeg"); // or use a library to detect mime type
    res.setHeader("X-Content-Type-Options", "nosniff");

    // Send the file if it exists
    res.sendFile(filePath, (err) => {
        if (err) {
            // Do not leak internal info
            return res.status(404).send("Image not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
