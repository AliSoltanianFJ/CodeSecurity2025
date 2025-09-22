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
  windowMs: 1 * 60 * 1000, // 1 minute window
  max: 30, // limit each IP to 30 requests per windowMs
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

// Add the rate limiter to the /img endpoint
app.use("/img", rateLimit);

app.get("/img", (req, res) => {
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
  const imagesDir = path.resolve(__dirname, "images");
  const imagePath = path.resolve(imagesDir, fileName);

  if (!imagePath.startsWith(imagesDir)) {
   return res.status(400).send("Invalid file path.");
  }

  // Set X-Content-Type-Options header to prevent MIME sniffing
  res.setHeader("X-Content-Type-Options", "nosniff");

  // Send the file if it exists
  res.sendFile(imagePath, err => {
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
