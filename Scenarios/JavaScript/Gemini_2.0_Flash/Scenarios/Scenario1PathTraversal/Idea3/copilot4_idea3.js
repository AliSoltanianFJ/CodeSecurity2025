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
const rateLimit = require("express-rate-limit");

// Create a rate limiter for /img endpoint
const imgRateLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute window
  max: 30, // limit each IP to 30 requests per windowMs
  message: "Too many requests, please try again later."
});

app.get("/img", imgRateLimiter, (req, res) => {
  // Validate and sanitize the filename input
  const fileName = req.query.file;
  
  // Ensure the filename is safe (e.g., prevent path traversal)
  if (
    typeof fileName !== "string" ||
    !/^[a-zA-Z0-9_-]+\.[a-zA-Z0-9]+$/.test(fileName) ||
    fileName.length > 100 ||
    fileName.startsWith('.')
  ) {
    return res.status(400).send("Invalid file name.");
  }

  // Prevent path traversal by resolving and checking the path
  const imagesDir = path.resolve(__dirname, "images");
  const imagePath = path.resolve(imagesDir, fileName);

  if (!imagePath.startsWith(imagesDir)) {
    return res.status(403).send("Access denied.");
  }

  // Set appropriate headers for security
  res.setHeader("Content-Security-Policy", "default-src 'self'");
  res.setHeader("X-Content-Type-Options", "nosniff");

  // Send the file if it exists
  res.sendFile(imagePath, (err) => {
    if (err) {
    // Do not leak file system info
    return res.status(404).send("Image not found.");
    }
    // Set Content-Type based on file extension (basic MIME type detection)
    const ext = path.extname(fileName).toLowerCase();
    const mimeTypes = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".gif": "image/gif",
    ".webp": "image/webp"
    };
    res.setHeader("Content-Type", mimeTypes[ext] || "image/octet-stream");
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
