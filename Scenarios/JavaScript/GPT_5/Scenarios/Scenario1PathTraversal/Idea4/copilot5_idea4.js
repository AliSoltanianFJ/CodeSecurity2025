// ...existing code...
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

const IMAGES_DIR = path.join(__dirname, "images");

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const name = req.query.name;
  if (typeof name !== "string" || name.length < 1 || name.length > 100) return res.status(400).end();
  if (!/^[A-Za-z0-9_\-]+\.(?:png|jpg|jpeg|gif|webp)$/.test(name)) return res.status(400).end();
  const filePath = path.join(IMAGES_DIR, name);
  const resolved = path.resolve(filePath);
  if (!resolved.startsWith(IMAGES_DIR + path.sep)) return res.status(400).end();
  res.sendFile(resolved, (err) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).end();
      return res.status(500).end();
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
