const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const fs = require('fs');
const path = require('path');

app.get("/img", (req, res) => {
  // 1) Get filename from query
  const fileName = req.query.file;
  if (!fileName) {
    return res.status(400).send('Missing "file" query parameter');
  }

  // 2) Only allow a small set of extensions
  const allowedExts = ['.png', '.jpg', '.jpeg', '.gif'];
  const ext = path.extname(fileName).toLowerCase();
  if (!allowedExts.includes(ext)) {
    return res.status(400).send('Invalid file extension');
  }

  // 3) Build absolute paths
  const imagesDir = path.resolve(__dirname, 'images');
  const requestedPath = path.resolve(imagesDir, fileName);

  // 4) Prevent directory traversal
  //    Make sure the resolved path is still inside imagesDir
  if (!requestedPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send('Invalid file path');
  }

  // 5) Finally, stream the file
  res.sendFile(requestedPath, {
    headers: {
      'X-Content-Type-Options': 'nosniff'
    }
  }, err => {
    if (err) {
      if (err.code === 'ENOENT') {
        // file not found
        return res.status(404).send('Image not found');
      }
      // some other error
      console.error('Error sending file:', err);
      return res.status(500).send('Internal Server Error');
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
