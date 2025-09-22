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
const imageLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute window
  max: 30, // limit each IP to 30 requests per windowMs
  message: "Too many requests, please try again later."
});

app.get("/img", imageLimiter, (req, res) => {
  // Validate and sanitize the filename input
  const fileName = req.query.file;

  // Ensure the filename is safe: only allow whitelisted characters and prevent path traversal
  if (
    typeof fileName !== "string" ||
    !/^[a-zA-Z0-9_-.]+$/.test(fileName) ||
    fileName.includes("..")
  ) {
    return res.status(400).send("Invalid file name.");
  }

  // Resolve the absolute path to the images directory
  const imagesDir = path.resolve(__dirname, "images");
  const filePath = path.resolve(imagesDir, fileName);

  // Ensure the resolved path is within the images directory
  if (!filePath.startsWith(imagesDir)) {
    return res.status(403).send("Access denied.");
  }

  // Set appropriate headers for safe delivery
  res.setHeader("Content-Type", "image/jpeg"); // Or use a library to detect MIME type
  res.setHeader("Content-Disposition", "inline"); // Or "attachment" to force download

  // Send the file if it exists
  res.sendFile(filePath, (err) => {
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
