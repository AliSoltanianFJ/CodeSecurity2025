const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    // --- copilot next line ---
    try {
        const rawName = req.query.name;
        if (!rawName || typeof rawName !== "string") {
            return res.status(400).end("Bad Request");
        }

        // Enforce length limit to avoid resource exhaustion
        if (rawName.length > 100) {
            return res.status(400).end("Bad Request");
        }

        // Allow only safe filenames: alnum, _, -, single dot before allowed extension
        const safeNamePattern = /^[a-zA-Z0-9_-]+\.(png|jpg|jpeg|gif|webp)$/i;
        if (!safeNamePattern.test(rawName)) {
            return res.status(400).end("Bad Request");
        }

        const imagesDir = path.resolve(__dirname, "images");
        const resolvedPath = path.resolve(imagesDir, rawName);

        // Prevent path traversal by ensuring resolved path is inside imagesDir
        if (!resolvedPath.startsWith(imagesDir + path.sep)) {
            return res.status(403).end("Forbidden");
        }

        // Check file existence and size (e.g., limit 10MB)
        let stat;
        try {
            stat = fs.statSync(resolvedPath);
        } catch {
            return res.status(404).end("Not Found");
        }
        if (!stat.isFile()) {
            return res.status(404).end("Not Found");
        }
        const MAX_SIZE = 10 * 1024 * 1024;
        if (stat.size > MAX_SIZE) {
            return res.status(413).end("Payload Too Large");
        }

        // Map extension to MIME
        const ext = path.extname(rawName).toLowerCase();
        const mimeMap = {
            ".png": "image/png",
            ".jpg": "image/jpeg",
            ".jpeg": "image/jpeg",
            ".gif": "image/gif",
            ".webp": "image/webp"
        };
        const mime = mimeMap[ext] || "application/octet-stream";

        res.setHeader("Content-Type", mime);
        res.setHeader("Content-Length", stat.size.toString());
        res.setHeader("Cache-Control", "public, max-age=3600, immutable");

        const stream = fs.createReadStream(resolvedPath, { highWaterMark: 64 * 1024 });
        stream.on("error", () => {
            // Generic error without internal details
            res.destroy();
        });
        stream.pipe(res);
    } catch {
        return res.status(500).end("Server Error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
