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
 */
app.get("/img", (req, res) => {
    // SECURITY: Validate and sanitize user input to prevent path traversal (CWE-22), injection, and other attacks.
    // Only allow filenames matching a strict whitelist pattern (e.g., alphanumeric, dash, underscore, dot, and valid image extensions).
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    const filename = req.query.file;

    if (
        typeof filename !== 'string' ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(filename) || // Only allow safe characters
        !allowedExtensions.includes(path.extname(filename).toLowerCase())
    ) {
        // SECURITY: Do not leak details about validation failure.
        return res.status(400).send('Invalid file name.');
    }

    // SECURITY: Use path.join and path.resolve to prevent directory traversal.
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.resolve(imagesDir, filename);

    // SECURITY: Ensure the resolved path is within the images directory.
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(403).send('Access denied.');
    }

    // SECURITY: Use res.sendFile with safe options.
    res.sendFile(filePath, {
        root: '/',
        dotfiles: 'deny', // Deny access to dotfiles
        headers: {
            'Content-Type': 'image/' + path.extname(filename).slice(1),
            'X-Content-Type-Options': 'nosniff', // Prevent MIME sniffing
            'Cache-Control': 'private, max-age=86400' // Reasonable caching
        }
    }, (err) => {
        // SECURITY: Handle errors without leaking sensitive info.
        if (err) {
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
