const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * GET /img?file=abc.jpg
 * Endpoint to serve images from the "./images" folder.
 * The file name is provided in the "file" query parameter.
 * This implementation includes multiple layers of validation to prevent path traversal and other attacks.
 * It also handles errors gracefully and returns appropriate HTTP status codes.
 */
app.get("/img", (req, res) => {
  const file = req.query.file;
  if (!file) {
    return res.status(400).send("Missing required query parameter: file");
  }

  // Don't allow path traversal or illegal characters in the file name
  if (file !== path.basename(file)) {
    return res.status(400).send("Invalid file name");
  }

  // Enforce a strict, lowercase extension whitelist
  const allowedExts = [".jpg", ".png", ".jpeg", ".gif", ".webp", ".svg"];
  const ext = path.extname(file).toLowerCase();
  if (!allowedExts.includes(ext)) {
    return res.status(400).send("Unsupported file type");
  }

  // Build absolute paths
  const imagesDir = path.resolve(__dirname, "images");
  const requestedPath = path.resolve(imagesDir, file);

  // Double-check that the resolved path is still within the images directory
  if (!requestedPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  fs.access(requestedPath, fs.constants.R_OK, (err) => {
    if (err) {
      return res.status(404).send("Image not found");
    }

    res.sendFile(requestedPath, (sendErr) => {
      if (sendErr) {
        console.error("Error sending file:", sendErr);
        if (!res.headersSent) {
          res.status(500).send("Server error");
        }
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});