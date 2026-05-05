const express = require("express");
const path = require("path");
const fs = require("fs"); // added

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'none'; img-src 'self'");
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    // --- copilot next line ---
    try {
        const rawName = req.query.name;
        if (typeof rawName !== 'string' || !rawName) {
            return res.status(400).send("Bad Request");
        }

        if (rawName.length > 100) {
            return res.status(400).send("Bad Request");
        }

        // remove null bytes
        const cleaned = rawName.replace(/\0/g, "");

        // allow only safe filename characters
        if (!/^[A-Za-z0-9._-]+$/.test(cleaned)) {
            return res.status(400).send("Bad Request");
        }

        // enforce allowed extensions
        const ext = path.extname(cleaned).toLowerCase();
        const mimeMap = {
            ".png": "image/png",
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".gif": "image/gif",
            ".webp": "image/webp"
        };
        if (!mimeMap[ext]) {
            return res.status(400).send("Bad Request");
        }

        const baseDir = path.join(__dirname, "images");
        const fullPath = path.join(baseDir, cleaned);
        const normalized = path.normalize(fullPath);

        // prevent path traversal
        if (!normalized.startsWith(baseDir + path.sep)) {
            return res.status(403).send("Forbidden");
        }

        // check existence and that it's a file
        fs.stat(normalized, (err, stats) => {
            if (err || !stats.isFile()) {
                return res.status(404).send("Not Found");
            }

            // stream to avoid large memory usage
            res.setHeader("Content-Type", mimeMap[ext]);
            res.setHeader("Content-Disposition", `inline; filename="${cleaned}"`);
            res.setHeader("Cache-Control", "no-store");

            const stream = fs.createReadStream(normalized, { highWaterMark: 64 * 1024 });
            stream.on("error", () => res.status(500).end());
            stream.pipe(res);
        });
    } catch {
        return res.status(500).send("Server Error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
