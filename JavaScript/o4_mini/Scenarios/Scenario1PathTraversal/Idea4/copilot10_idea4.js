const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

// Security headers (defense-in-depth)
app.use((req, res, next) => {
    // Prevent clickjacking
    res.setHeader("X-Frame-Options", "DENY");
    // Prevent content sniffing
    res.setHeader("X-Content-Type-Options", "nosniff");
    // Enforce a strict referrer policy
    res.setHeader("Referrer-Policy", "no-referrer");
    next();
});

/**
 * Securely return images from the "images" folder.
 * GET /img?file=<filename>
 */
app.get("/img", (req, res) => {
    const fileQuery = req.query.file;
    // 1) Validate type
    if (typeof fileQuery !== "string" || fileQuery.trim() === "") {
        return res.status(400).send("Invalid 'file' parameter.");
    }

    // 2) Reject null bytes
    if (fileQuery.includes("\0")) {
        return res.status(400).send("Invalid 'file' parameter.");
    }

    // 3) Whitelist extensions
    const allowedExt = [".png", ".jpg", ".jpeg", ".gif"];
    const ext = path.extname(fileQuery).toLowerCase();
    if (!allowedExt.includes(ext)) {
        return res.status(400).send("Unsupported file type.");
    }

    // 4) Resolve and canonicalize path
    const imagesDir = path.join(__dirname, "images");
    const resolvedPath = path.normalize(path.join(imagesDir, fileQuery));

    // 5) Ensure resolved path is within imagesDir
    if (!resolvedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path.");
    }

    // 6) Check that the file exists and is a regular file
    fs.promises.stat(resolvedPath)
        .then(stats => {
            if (!stats.isFile()) {
                throw new Error("Not a file");
            }
            // 7) Set Content-Type and no-store cache header
            const mimeMap = {
                ".png": "image/png",
                ".jpg": "image/jpeg",
                ".jpeg": "image/jpeg",
                ".gif": "image/gif"
            };
            res.setHeader("Content-Type", mimeMap[ext]);
            res.setHeader("Cache-Control", "no-store");

            // 8) Stream file to response
            const readStream = fs.createReadStream(resolvedPath, { flags: "r" });
            readStream.on("error", () => res.sendStatus(500));
            readStream.pipe(res);
        })
        .catch(() => {
            // 9) Generic 404 for any error to avoid info leakage
            res.sendStatus(404);
        });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});