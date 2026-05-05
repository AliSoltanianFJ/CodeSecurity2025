const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

// Deny framing to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * GET /img?name=<filename>
 * Serves images from the "./images" folder.
 */
app.get("/img", (req, res) => {
  // 1) Get the file name from the query string
  const fileName = req.query.name;
  if (!fileName) {
    return res.status(400).send("Missing 'name' query parameter");
  }

  // 2) Reject anything that looks like a path traversal or illegal characters
  //    Only allow alphanumeric, underscore, hyphen and a single dot before the extension.
  const validNameRe = /^[a-zA-Z0-9_-]+\.(jpg|jpeg|png|gif)$/i;
  if (!validNameRe.test(fileName)) {
    return res.status(400).send("Invalid file name");
  }

  // 3) Build an absolute path to the images directory
  const imagesDir = path.resolve(__dirname, "images");
  // 4) Resolve the requested file’s absolute path
  const resolvedPath = path.resolve(imagesDir, fileName);

  // 5) Ensure the resolved path is still within our images directory
  if (!resolvedPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 6) Check existence and that it’s a file
  fs.stat(resolvedPath, (err, stats) => {
    if (err) {
      // File does not exist or other filesystem error
      if (err.code === "ENOENT") {
        return res.status(404).send("Image not found");
      }
      return res.status(500).send("Server error");
    }
    if (!stats.isFile()) {
      return res.status(404).send("Not a file");
    }

    // 7) Serve the file
    res.sendFile(resolvedPath, (err) => {
      if (err) {
        console.error("Error sending file:", err);
        // If headers aren’t sent yet, send a 500
        if (!res.headersSent) {
          res.status(500).send("Error sending file");
        }
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});