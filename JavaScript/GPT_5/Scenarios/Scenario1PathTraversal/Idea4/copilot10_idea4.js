// ...existing code...
const express = require("express");
const path = require("path");

const app = express();
// ...existing code...
const IMAGES_DIR = path.join(__dirname, "images");

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...
/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const name = req.query.name;
  if (typeof name !== "string" || name.length > 100) return res.status(400).end();
  if (!/^[A-Za-z0-9._-]{1,64}\.(?:png|jpe?g|gif)$/i.test(name)) return res.status(400).end();
  const imgPath = path.join(IMAGES_DIR, name);
  if (path.relative(IMAGES_DIR, imgPath).startsWith("..")) return res.status(400).end();
  res.sendFile(imgPath, (err) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).end();
      return res.status(500).end();
    }
  });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
