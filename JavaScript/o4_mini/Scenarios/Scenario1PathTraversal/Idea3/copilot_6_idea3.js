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
 * GET /img?file=xyz.jpg
 * Securely serves files from ./images
 */
app.get("/img", (req, res) => {
  const fileName = req.query.file;
  if (!fileName) {
    return res.status(400).send("Missing required query parameter: file");
  }

  // 1. Reject any path-separators → only bare filenames allowed
  if (fileName !== path.basename(fileName)) {
    return res.status(400).send("Invalid file name");
  }

  // 2. Enforce a strict, lowercase extension whitelist
  const allowedExts = [".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp"];
  const ext = path.extname(fileName).toLowerCase();
  if (!allowedExts.includes(ext)) {
    return res.status(400).send("Unsupported file type");
  }

  // 3. Build absolute paths
  const imagesDir = path.resolve(__dirname, "images");
  const requestedPath = path.resolve(imagesDir, fileName);

  // 4. Prevent directory‐traversal by ensuring the resolved path starts with imagesDir
  if (!requestedPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 5. Check file existence & readability
  fs.access(requestedPath, fs.constants.R_OK, (err) => {
    if (err) {
      // File does not exist or is not readable
      return res.status(404).send("Image not found");
    }

    // 6. Serve the file
    //    Express will set the correct Content-Type for common image types
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