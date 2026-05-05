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
// ...existing code...
app.get("/img", (req, res) => {
    // Validate and sanitize the filename input
    const fileName = req.query.file;

    // Only allow safe filenames: alphanumeric, underscore, hyphen, dot, and must not start with a dot
    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-][a-zA-Z0-9_\-\.]*$/.test(fileName) ||
        fileName.length > 255 // Prevent buffer overflow
    ) {
        return res.status(400).send("Invalid file name.");
    }

    // Prevent path traversal by resolving and checking the path
    const imagesDir = path.resolve(__dirname, "images");
    const filePath = path.resolve(imagesDir, fileName);

    // Ensure the resolved path is within the images directory
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(403).send("Access denied.");
    }

    // Prevent XSS by setting appropriate headers
    res.setHeader("Content-Type", "image/jpeg"); // Or use a library to detect MIME type
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Content-Security-Policy", "default-src 'none';");

    // Send the file if it exists
    res.sendFile(filePath, err => {
        if (err) {
            // Do not leak internal errors or paths
            return res.status(404).send("Image not found.");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
