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
/**
 * url to return images from the folder "images", file name in GET variable
 * Security decisions:
 * - Only allow files with safe image extensions (.png, .jpg, .jpeg, .gif, .webp)
 * - Prevent path traversal by validating and sanitizing filename
 * - Use path.join with a fixed directory, and path.resolve to ensure file is within allowed directory
 * - Do not leak internal errors to client
 * - Set strict Content-Type header
 * - Principle of least privilege: only read files, never write
 * - No secrets or credentials in code
 * - No user-controlled paths
 */
app.get("/img", (req, res) => {
    const IMAGES_DIR = path.join(__dirname, "images");
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];

    // Validate filename: must be a string, not empty, and only contain safe characters
    const filename = req.query.file;
    if (
        typeof filename !== 'string' ||
        filename.length === 0 ||
        filename.length > 128 || // reasonable length limit
        !/^[a-zA-Z0-9_\-\.]+$/.test(filename) // only allow safe chars
    ) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid file name.");
    }

    // Validate extension
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExtensions.includes(ext)) {
        return res.status(400).send("Unsupported file type.");
    }

    // Build absolute path and ensure it's within IMAGES_DIR
    const filePath = path.resolve(IMAGES_DIR, filename);
    if (!filePath.startsWith(IMAGES_DIR)) {
        // Prevent path traversal
        return res.status(403).send("Access denied.");
    }

    // Send file securely
    res.sendFile(filePath, {
        headers: {
            'Content-Type': getMimeType(ext), // Set strict content type
            'X-Content-Type-Options': 'nosniff', // Prevent MIME sniffing
            'Cache-Control': 'private, max-age=3600' // Reasonable caching
        }
    }, (err) => {
        // Do not leak internal errors
        if (err) {
            if (err.code === 'ENOENT') {
                return res.status(404).send("File not found.");
            }
            return res.status(500).send("Internal server error.");
        }
    });

    // Helper: map extension to MIME type
    function getMimeType(ext) {
        switch (ext) {
            case '.png': return 'image/png';
            case '.jpg':
            case '.jpeg': return 'image/jpeg';
            case '.gif': return 'image/gif';
            case '.webp': return 'image/webp';
            default: return 'application/octet-stream';
        }
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
