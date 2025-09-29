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
 * - Use absolute path resolution and check that the resolved path is within the images directory
 * - Do not leak internal errors to the client
 * - Set strict Content-Type and Content-Disposition headers
 * - Do not cache responses
 */
app.get("/img", (req, res) => {
    const IMAGES_DIR = path.join(__dirname, "images");
    const SAFE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];

    // Validate filename: must be a non-empty string, only contain safe characters, and have a safe extension
    const filename = req.query.file;
    if (
        typeof filename !== 'string' ||
        filename.length === 0 ||
        filename.length > 128 || // Prevent DoS via long filenames
        !/^[a-zA-Z0-9_\-\.]+$/.test(filename) || // Only allow safe characters
        !SAFE_EXTENSIONS.includes(path.extname(filename).toLowerCase())
    ) {
        // Do not reveal details about validation failure
        return res.status(400).send("Invalid file request.");
    }

    // Resolve absolute path and ensure it's within IMAGES_DIR
    const filePath = path.resolve(IMAGES_DIR, filename);
    if (!filePath.startsWith(IMAGES_DIR)) {
        // Path traversal attempt
        return res.status(403).send("Access denied.");
    }

    // Set security headers
    res.setHeader('Content-Type', 'application/octet-stream'); // Will be overridden by sendFile if possible
    res.setHeader('Content-Disposition', `inline; filename="${path.basename(filename)}"`);
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');

    // Send file securely
    res.sendFile(filePath, {
        root: '/',
        dotfiles: 'deny',
        headers: {
            'X-Content-Type-Options': 'nosniff'
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
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
