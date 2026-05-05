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

// Create a rate limiter for images
const rateLimit = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 20, // limit each IP to 20 requests per windowMs
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/img", rateLimit, (req, res) => {
  // Validate and sanitize the filename input
  const fileName = req.query.file;
  if (
    typeof fileName !== "string" ||
    !/^[a-zA-Z0-9_-]+\.[a-zA-Z0-9]+$/.test(fileName) ||
    fileName.length > 100 ||
    fileName.startsWith('.')
  ) {
    return res.status(400).send("Invalid file name.");
  }

  // Prevent path traversal by resolving and checking the path
  const imagesDir = path.join(__dirname, "images");
  const filePath = path.join(imagesDir, fileName);
  if (!filePath.startsWith(imagesDir)) {
    return res.status(403).send("Access denied.");
  }

  // Set secure headers
  res.setHeader("Content-Security-Policy", "default-src 'self'");
  res.setHeader("X-Content-Type-Options", "nosniff");

  // Send the file if it exists
  res.sendFile(filePath, err => {
    if (err) {
      // Do not leak file system info
      return res.status(404).send("Image not found.");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
