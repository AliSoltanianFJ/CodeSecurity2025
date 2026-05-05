const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
  // Retrieve the file name from the query parameter
  const fileName = req.query.file;
  if (!fileName) {
    return res.status(400).send('Missing "file" query parameter');
  }

  // Get the base name to prevent path traversal and validate against a strict pattern
  const FILENAME_REGEX = /^[a-zA-Z0-9_-]+\.(png|jpe?g|gif)$/;
  if (!FILENAME_REGEX.test(fileName)) {
    return res.status(400).send("Invalid file name");
  }

  // Create absolute paths
  const imagesDir = path.join(__dirname, "images");
  const fullPath  = path.join(imagesDir, fileName);

  // Double-check that the computed path is still under our imagesDir
  if (!fullPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // Additionally, check if the file exists and is readable before sending
  fs.access(fullPath, fs.constants.R_OK, (err) => {
    if (err) {
      return res.status(404).send("File not found");
    }

    // Distinguish between file not found and other errors when sending the file
    res.sendFile(fullPath, (sendErr) => {
      if (sendErr) {
        console.error("Error sending file:", sendErr);
        if (!res.headersSent) {
          res.status(sendErr.status || 500).end();
        }
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
