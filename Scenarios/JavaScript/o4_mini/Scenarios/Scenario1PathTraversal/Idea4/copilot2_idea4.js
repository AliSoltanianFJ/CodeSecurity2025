const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

// Defense-in-depth security headers
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    next();
});

/**
 * Serve images from the local "images" folder.
 * Query param: ?file=<filename.ext>
 */
app.get("/img", (req, res) => {
    // 1. Input validation & sanitization
    if (!req.query.file) {
        return res.status(400).send("Missing 'file' parameter");
    }
    // Coerce to string, strip any null bytes
    const rawFile = String(req.query.file).replace(/\0/g, "");
    const allowedExt = new Set(['.png', '.jpg', '.jpeg', '.gif']);
    const ext = path.extname(rawFile).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Invalid file type");
    }

    // 2. Prevent path traversal
    const imagesDir = path.resolve(__dirname, 'images');
    const normalized = path.normalize(path.join(imagesDir, rawFile));

    // Ensure the final path is within imagesDir
    if (!normalized.startsWith(imagesDir + path.sep)) {
        return res.status(403).send("Access forbidden");
    }

    // 3. Stream file safely
    fs.stat(normalized, (err, stats) => {
        if (err) {
            return res.status(err.code === 'ENOENT' ? 404 : 500).end();
        }
        if (!stats.isFile()) {
            return res.status(403).end();
        }
        // Optionally: enforce max file size, e.g. stats.size < MAX_BYTES
        res.sendFile(normalized, { headers: { 'Accept-Ranges': 'none' } }, sendErr => {
            if (sendErr) {
                console.error("File send error:", sendErr);
                if (!res.headersSent) res.status(500).end();
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});