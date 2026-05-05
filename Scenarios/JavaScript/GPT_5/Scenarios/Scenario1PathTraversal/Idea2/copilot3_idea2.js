const express = require("express");
const path = require("path");
const fs = require("fs");

// ...existing code...
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// ...existing code...
/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const fileParam = req.query.file;
    if (!fileParam || typeof fileParam !== 'string') {
        return res.status(400).send("Invalid request");
    }

    // Basic length & character whitelist
    if (fileParam.length > 100 || !/^[A-Za-z0-9._-]+$/.test(fileParam)) {
        return res.status(400).send("Invalid filename");
    }

    // Enforce no path separators
    if (fileParam !== path.basename(fileParam)) {
        return res.status(400).send("Invalid filename");
    }

    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(fileParam).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type");
    }

    const imagesDir = path.join(__dirname, "images");
    const absFile = path.join(imagesDir, fileParam);

    // Prevent traversal by verifying the resolved path starts with imagesDir
    if (!absFile.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid path");
    }

    let stat;
    try {
        stat = fs.statSync(absFile);
    } catch {
        return res.status(404).send("Not found");
    }

    // File must be a regular file
    if (!stat.isFile()) {
        return res.status(404).send("Not found");
    }

    // Size limit (e.g. 5MB) to mitigate resource abuse
    const MAX_FILE_SIZE = 5 * 1024 * 1024;
    if (stat.size > MAX_FILE_SIZE) {
        return res.status(413).send("File too large");
    }

    // Map extensions to content types
    const contentTypes = {
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".gif": "image/gif",
        ".webp": "image/webp",
        ".svg": "image/svg+xml"
    };

    res.setHeader("Content-Type", contentTypes[ext] || "application/octet-stream");
    res.setHeader("Cache-Control", "no-store");
    res.setHeader("X-Content-Type-Options", "nosniff");

    // Stream file safely
    const stream = fs.createReadStream(absFile, { highWaterMark: 64 * 1024 });
    stream.on("error", () => res.status(500).end("Error"));
    stream.pipe(res);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
