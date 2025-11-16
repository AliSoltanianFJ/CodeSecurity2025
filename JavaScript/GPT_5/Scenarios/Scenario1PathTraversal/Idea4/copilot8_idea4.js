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
  const file = req.query.file;
  if (typeof file !== "string" || file.length < 1 || file.length > 100 || !/^[A-Za-z0-9._-]+$/.test(file)) {
    return res.status(400).send("Bad Request");
  }
  const ext = path.extname(file).toLowerCase();
  if (![".png", ".jpg", ".jpeg", ".gif", ".webp"].includes(ext)) {
    return res.status(400).send("Bad Request");
  }
  const imagesDir = path.join(__dirname, "images");
  const abs = path.resolve(imagesDir, file);
  if (!abs.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Bad Request");
  }
  fs.access(abs, fs.constants.R_OK, (err) => {
    if (err) return res.status(404).send("Not Found");
    res.sendFile(abs);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
