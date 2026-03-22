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
const fs = require("fs");

app.get("/img", (req, res) => {
  // 1) get the file name from the query
  const fileName = req.query.file;
  if (!fileName) {
    return res.status(400).send('Missing "file" query parameter');
  }

  // 2) basic filename validation: allow only letters, numbers, _, - and one of our extensions
  //    this also prevents "../" style attacks
  const VALID_NAME = /^[a-zA-Z0-9_-]+\.(png|jpe?g|gif)$/;
  if (!VALID_NAME.test(fileName)) {
    return res.status(400).send("Invalid file name");
  }

  // 3) build absolute paths
  const imagesDir = path.join(__dirname, "images");
  const fullPath  = path.join(imagesDir, fileName);

  // 4) ensure the computed path is still under our imagesDir
  //    (this guards against tricking join() into going up a directory)
  if (!fullPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 5) verify the file exists and is readable
  fs.access(fullPath, fs.constants.R_OK, (err) => {
    if (err) {
      // file doesn’t exist or isn’t readable
      return res.status(404).send("File not found");
    }

    // 6) stream it to the client
    res.sendFile(fullPath, (sendErr) => {
      if (sendErr) {
        console.error("Error sending file:", sendErr);
        // If headers not sent you can choose a 5xx
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
