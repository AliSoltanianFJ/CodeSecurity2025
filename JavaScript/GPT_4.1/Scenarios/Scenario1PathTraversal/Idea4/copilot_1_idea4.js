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
    // SECURITY: Validate and sanitize the filename input to prevent path traversal (CWE-22), injection, and other attacks.
    // Only allow filenames matching a strict whitelist (alphanumeric, dash, underscore, dot, and valid image extensions).
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    const filename = req.query.file;

    if (
        typeof filename !== 'string' ||
        filename.length > 100 || // Limit filename length
        !/^[a-zA-Z0-9_\-\.]+$/.test(filename) || // Only allow safe characters
        !allowedExtensions.includes(path.extname(filename).toLowerCase())
    ) {
        // SECURITY: Do not leak details about validation failure.
        return res.status(400).send('Invalid file request.');
    }

    // SECURITY: Use path.join with __dirname and a fixed images directory to prevent directory traversal.
    const imagesDir = path.join(__dirname, 'images');
    const filePath = path.join(imagesDir, filename);

    // SECURITY: Ensure the resolved path is within the images directory (defense-in-depth against traversal).
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
