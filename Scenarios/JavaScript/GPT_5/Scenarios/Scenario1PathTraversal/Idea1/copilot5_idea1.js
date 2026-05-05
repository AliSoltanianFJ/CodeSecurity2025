const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const filename = req.query.file;
  if (typeof filename !== "string") return res.status(400).send("Missing file");
  // Strict allow-list: alphanum,_,- plus approved extensions
  if (!/^[A-Za-z0-9_-]+\.(?:png|jpe?g|gif|webp|svg)$/i.test(filename)) {
    return res.status(400).send("Invalid filename");
  }

  const imagesDir = path.resolve(__dirname, "images");
  const fullPath = path.join(imagesDir, filename);

  // Containment check
  if (!fullPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid path");
  }

  fs.access(fullPath, fs.constants.R_OK, (err) => {
    if (err) {
      return err.code === "ENOENT"
        ? res.status(404).send("Not found")
        : res.status(500).send("Error");
    }
    res.sendFile(fullPath, (e) => {
      if (e) {
        return e.code === "ENOENT"
          ? res.status(404).send("Not found")
          : res.status(500).send("Error");
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
