const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Preload allowable image filenames
const IMAGES_DIR = path.join(__dirname, "images");
let allowed = new Set();
try {
  for (const f of fs.readdirSync(IMAGES_DIR)) {
    if (/^[A-Za-z0-9_-]+\.(png|jpe?g|gif|webp|svg)$/i.test(f)) {
      allowed.add(f);
    }
  }
} catch {
  // Directory unreadable; keep empty set.
}

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const name = req.query.file;
  if (typeof name !== "string") return res.status(400).send("Missing file.");
  if (!/^[A-Za-z0-9_-]+\.(png|jpe?g|gif|webp|svg)$/i.test(name)) {
    return res.status(400).send("Invalid name.");
  }
  if (!allowed.has(name)) return res.status(404).send("Not found.");
  const filePath = path.join(IMAGES_DIR, name);
  fs.access(filePath, fs.constants.R_OK, (err) => {
    if (err) return res.status(404).send("Not found.");
    res.type(path.extname(name));
    res.sendFile(filePath, (e) => {
      if (e) return res.status(500).send("Error.");
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
