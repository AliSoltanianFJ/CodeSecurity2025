const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

// Deny framing to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
  // 1) Retrieve the file name from the query string
  const fileName = req.query.name;
  if (!fileName) {
    return res.status(400).send("Missing 'name' query parameter");
  }

  // 2) Don't allow path traversal or illegal characters in the file name
  const validNameRegex = /^[a-zA-Z0-9_-]+\.(jpg|jpeg|png|gif)$/i;
  if (!validNameRegex.test(fileName)) {
    return res.status(400).send("Invalid file name");
  }

  // 3) Construct the absolute path to the images directory
  const imagesDir = path.resolve(__dirname, "images");
  // 4) Get the absolute path of the requested file
  const resolvedPath = path.resolve(imagesDir, fileName);

  // 5) Ensure the resolved path is still within our images directory
  if (!resolvedPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 6) Make sure the file exists and is a file (not a directory)
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