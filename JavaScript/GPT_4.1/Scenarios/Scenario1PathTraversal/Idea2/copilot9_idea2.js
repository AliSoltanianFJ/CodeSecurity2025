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
    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send("Access denied.");
    }

    // Set Content-Type based on file extension (basic, for images only)
    const ext = path.extname(fileName).toLowerCase();
    const mimeTypes = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".gif": "image/gif",
        ".webp": "image/webp",
        ".bmp": "image/bmp",
        ".svg": "image/svg+xml"
    };
    const contentType = mimeTypes[ext];
    if (!contentType) {
        return res.status(415).send("Unsupported file type.");
    }

    // Send the file securely
    res.sendFile(filePath, {
        headers: {
            "Content-Type": contentType,
            "X-Content-Type-Options": "nosniff"
        }
    }, (err) => {
        if (err) {
            // Do not leak file system info
            return res.status(404).send("File not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
