// ...existing code...
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const IMAGES_DIR = path.join(__dirname, "images");

// ...existing code...
/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const file = req.query.file;
  if (typeof file !== "string" || file.length === 0 || file.length > 100) return res.status(400).end();
  if (!/^[A-Za-z0-9._-]+\.(png|jpe?g|gif|webp)$/i.test(file)) return res.status(400).end();
  const full = path.join(IMAGES_DIR, file);
  if (!full.startsWith(IMAGES_DIR + path.sep)) return res.status(400).end();
  res.sendFile(full, (err) => {
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
