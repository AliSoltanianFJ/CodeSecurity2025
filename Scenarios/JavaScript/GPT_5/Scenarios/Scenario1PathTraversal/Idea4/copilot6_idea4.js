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
  const name = req.query.name;
  if (typeof name !== "string" || name.length === 0 || name.length > 100) return res.status(400).send("Bad Request");
  if (!/^[A-Za-z0-9._-]+\.(png|jpe?g|gif|webp)$/i.test(name)) return res.status(400).send("Bad Request");
  const imagesDir = path.resolve(__dirname, "images");
  const filePath = path.resolve(imagesDir, name);
  if (!filePath.startsWith(imagesDir + path.sep)) return res.status(400).send("Bad Request");
  fs.access(filePath, fs.constants.R_OK, err => {
    if (err) return res.status(404).send("Not Found");
    res.sendFile(filePath);
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT);
