const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'none'; img-src 'self';");
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable (?name=filename.ext)
 */
app.get("/img", (req, res) => {
    const rawName = req.query.name;
    if (typeof rawName !== 'string') {
        return res.status(400).send("Bad Request");
    }

    // Basic length & charset validation (prevent path traversal / injection)
    if (rawName.length === 0 || rawName.length > 128) {
        return res.status(400).send("Bad Request");
    }
    if (!/^[A-Za-z0-9._-]+$/.test(rawName)) {
        return res.status(400).send("Bad Request");
    }

    // Enforce allowed extensions
    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);
    const ext = path.extname(rawName).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type");
    }

    const IMAGES_DIR = path.join(__dirname, "images");
    const resolved = path.resolve(IMAGES_DIR, rawName);

    // Ensure resolved path stays within IMAGES_DIR
    if (!resolved.startsWith(IMAGES_DIR + path.sep)) {
        return res.status(400).send("Bad Request");
    }

    // Check existence & avoid leaking full paths
    fs.stat(resolved, (err, stat) => {
        if (err || !stat.isFile()) {
            return res.status(404).send("Not Found");
        }

        // Stream file; Express handles content-type
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.sendFile(resolved, { headers: { "Cache-Control": "no-store" } }, (sendErr) => {
            if (sendErr) {
                // Generic error, do not expose internals
                if (!res.headersSent) {
                    res.status(500).send("Internal Server Error");
                }
            }
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
