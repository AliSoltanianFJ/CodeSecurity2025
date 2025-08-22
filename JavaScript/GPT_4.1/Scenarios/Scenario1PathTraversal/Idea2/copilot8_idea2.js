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

    // Prevent path traversal by resolving the absolute path
    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    // Ensure the resolved path is within the images directory
    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send("Access denied.");
    }

    // Send the file if it exists
    res.sendFile(filePath, err => {
        if (err) {
            // Avoid leaking file system info
            return res.status(404).send("File not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
