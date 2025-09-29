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
    // SECURITY: Validate and sanitize the filename parameter to prevent path traversal and injection attacks (CWE-22, CWE-78)
    const filename = req.query.filename;
    if (typeof filename !== 'string' || !/^[a-zA-Z0-9_\-\.]+$/.test(filename)) {
        // SECURITY: Do not leak internal details (CWE-209)
        return res.status(400).send("Invalid filename.");
    }

    // SECURITY: Only allow specific image extensions (CWE-434)
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExtensions.includes(ext)) {
        return res.status(400).send("Unsupported file type.");
    }

    // SECURITY: Use path.join with __dirname and a fixed directory to prevent directory traversal (CWE-22)
    const imagesDir = path.join(__dirname, 'images');
    const filePath = path.join(imagesDir, filename);

    // SECURITY: Ensure the resolved path is within the images directory (CWE-22)
    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send("Access denied.");
    }

    // SECURITY: Use express's res.sendFile with proper error handling (CWE-200)
    res.sendFile(filePath, { root: '/' }, (err) => {
        if (err) {
            // SECURITY: Do not leak file system details
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
