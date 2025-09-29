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
    // SECURITY: Validate and sanitize the filename input to prevent path traversal (CWE-22), injection, and other attacks.
    // Only allow filenames matching a strict whitelist pattern (e.g., alphanumeric, dash, underscore, dot, and valid image extensions).
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    const filename = req.query.file;

    if (typeof filename !== 'string') {
        // SECURITY: Do not leak internal details.
        return res.status(400).send('Invalid file parameter.');
    }

    // SECURITY: Only allow safe filenames (no path separators, no traversal, no null bytes, etc.)
    const safeFilenamePattern = /^[a-zA-Z0-9_\-\.]+$/;
    if (!safeFilenamePattern.test(filename)) {
        return res.status(400).send('Invalid filename.');
    }

    // SECURITY: Ensure the extension is allowed.
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExtensions.includes(ext)) {
        return res.status(400).send('Unsupported file type.');
    }

    // SECURITY: Use path.join with a fixed directory, and path.resolve to ensure the file is inside the images folder.
    const imagesDir = path.join(__dirname, 'images');
    const filePath = path.resolve(imagesDir, filename);

    // SECURITY: Prevent directory traversal by ensuring the resolved path starts with the images directory.
    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send('Access denied.');
    }

    // SECURITY: Use express's res.sendFile with proper error handling.
    res.sendFile(filePath, { root: '/' }, (err) => {
        if (err) {
            // SECURITY: Do not leak file system details.
            if (err.code === 'ENOENT') {
                return res.status(404).send('File not found.');
            }
            return res.status(500).send('Internal server error.');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
