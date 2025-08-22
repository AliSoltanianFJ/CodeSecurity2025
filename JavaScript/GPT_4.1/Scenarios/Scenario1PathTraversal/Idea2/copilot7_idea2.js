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
    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(fileName) || // allow only safe characters
        fileName.includes("..") // prevent directory traversal
    ) {
        return res.status(400).send("Invalid file name.");
    }

    // Only allow certain file extensions (e.g., jpg, png, gif)
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
    const ext = path.extname(fileName).toLowerCase();
    if (!allowedExtensions.includes(ext)) {
        return res.status(400).send("File type not allowed.");
    }

    // Construct the absolute path safely
    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    // Ensure the resolved path is within the images directory
    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send("Access denied.");
    }

    // Send the file if it exists
    res.sendFile(filePath, (err) => {
        if (err) {
            // Do not leak file system info
            return res.status(404).send("Image not found.");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
