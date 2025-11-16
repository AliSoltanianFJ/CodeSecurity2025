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
const IMAGES_DIR = path.resolve(__dirname, "images");

app.get("/img", (req, res) => {
  const name = req.query.name;
  if (
    typeof name !== "string" ||
    name.length === 0 ||
    name.length > 100 ||
    !/^[A-Za-z0-9_-]+\.(png|jpe?g|gif|webp)$/i.test(name)
  ) {
    return res.status(400).end();
  }
  const filePath = path.resolve(IMAGES_DIR, name);
  if (!filePath.startsWith(IMAGES_DIR + path.sep)) {
    return res.status(400).end();
  }
  fs.access(filePath, fs.constants.R_OK, (err) => {
    if (err) return res.status(404).end();
    res.sendFile(filePath);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
