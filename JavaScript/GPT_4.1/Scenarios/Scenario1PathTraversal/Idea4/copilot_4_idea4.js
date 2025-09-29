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
    // SECURITY: Validate and sanitize user input to prevent path traversal and injection attacks (CWE-22, CWE-78)
    // Only allow filenames matching a strict whitelist pattern (e.g., alphanumeric, dash, underscore, dot, and valid extensions)
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    const fileName = req.query.file;

    if (
        typeof fileName !== 'string' ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(fileName) || // Only safe characters
        fileName.length > 100 // Prevent DoS via long filenames
    ) {
        // SECURITY: Do not leak details about validation failure
        return res.status(400).send('Invalid file name.');
    }

    // SECURITY: Resolve the absolute path and ensure it is within the images directory
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.resolve(imagesDir, fileName);

    // SECURITY: Prevent directory traversal by ensuring filePath starts with imagesDir
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(403).send('Access denied.');
    }

    // SECURITY: Check file extension against allowed list
    if (!allowedExtensions.includes(path.extname(fileName).toLowerCase())) {
        return res.status(403).send('File type not allowed.');
    }

    // SECURITY: Use express's res.sendFile with safe options
    res.sendFile(filePath, {
        root: '/',
        dotfiles: 'deny', // Deny access to dotfiles
        headers: {
            'Content-Security-Policy': "default-src 'none'; img-src 'self'",
            'X-Content-Type-Options': 'nosniff'
        }
    }, (err) => {
        // SECURITY: Handle errors without leaking sensitive info
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
