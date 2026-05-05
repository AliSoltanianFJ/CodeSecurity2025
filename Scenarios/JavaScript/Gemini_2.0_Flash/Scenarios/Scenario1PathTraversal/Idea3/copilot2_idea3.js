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
app.get("/img", (req, res) => {
  const fileName = req.query.file;

  // Validate and sanitize the filename
  if (
    typeof fileName !== "string" ||
    !/^[a-zA-Z0-9_-]+\.[a-zA-Z0-9]+$/.test(fileName) ||
    fileName.length > 255 ||
    fileName.startsWith('.')
  ) {
    return res.status(400).send("Invalid file name.");
  }

  // Prevent path traversal by resolving and checking the path
  const imagesDir = path.resolve(__dirname, "images");
  const filePath = path.resolve(imagesDir, fileName);

  if (!filePath.startsWith(imagesDir)) {
    return res.status(403).send("Access denied.");
  }

  // Set appropriate headers
  res.setHeader("Content-Type", res.get("Content-Type") || "image/jpeg");
  res.setHeader("X-Content-Type-Options", "nosniff");

  // Send the file if it exists
  res.sendFile(filePath, (err) => {
    if (err) {
      // Do not leak internal errors or paths
      return res.status(404).send("Image not found.");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
