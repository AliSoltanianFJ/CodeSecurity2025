const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// Define and lock down images directory
const IMAGES_DIR = path.resolve(__dirname, "images");
const ALLOWED_EXT = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
const MAX_NAME_LEN = 100;
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

/**
 * Secure image retrieval endpoint.
 * GET /img?file=<filename>
 */
app.get("/img", async (req, res) => {
  try {
    const fileParam = req.query.file;

    if (typeof fileParam !== "string" || fileParam.length === 0 || fileParam.length > MAX_NAME_LEN) {
      return res.status(400).send("Bad Request");
    }

    // Allow only safe characters
    if (!/^[A-Za-z0-9._-]+$/.test(fileParam)) {
      return res.status(400).send("Bad Request");
    }

    // Disallow suspicious sequences
    if (fileParam.includes("..")) {
      return res.status(400).send("Bad Request");
    }

    const ext = path.extname(fileParam).toLowerCase();
    if (!ALLOWED_EXT.has(ext)) {
      return res.status(415).send("Unsupported Media Type");
    }

    // Resolve and ensure path stays inside IMAGES_DIR
    const requestedPath = path.resolve(IMAGES_DIR, fileParam);
    if (!requestedPath.startsWith(IMAGES_DIR + path.sep)) {
      return res.status(403).send("Forbidden");
    }

    let stat;
    try {
      stat = await fs.promises.stat(requestedPath);
    } catch {
      return res.status(404).send("Not Found");
    }

    if (!stat.isFile()) {
      return res.status(404).send("Not Found");
    }

    if (stat.size > MAX_FILE_SIZE_BYTES) {
      return res.status(413).send("Payload Too Large");
    }

    // Basic content type mapping
    const mimeMap = {
      ".png": "image/png",
      ".jpg": "image/jpeg",
      ".jpeg": "image/jpeg",
      ".gif": "image/gif",
      ".webp": "image/webp",
      ".svg": "image/svg+xml"
    };
    const contentType = mimeMap[ext] || "application/octet-stream";

    res.setHeader("Content-Type", contentType);
    res.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; sandbox");
    res.setHeader("Cache-Control", "public, max-age=3600, immutable");

    const stream = fs.createReadStream(requestedPath, { highWaterMark: 64 * 1024 });
    stream.on("error", () => res.status(500).end());
    stream.pipe(res);
  } catch {
    return res.status(500).send("Server Error");
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
