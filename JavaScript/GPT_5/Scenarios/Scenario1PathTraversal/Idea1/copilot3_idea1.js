// ...existing code...
const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Pre-index allowed image files (whitelist approach)
const imagesDir = path.resolve(__dirname, "images");
let imageIndex = Object.create(null);
try {
  for (const f of fs.readdirSync(imagesDir)) {
    const ext = path.extname(f).toLowerCase();
    if ([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"].includes(ext)) {
      imageIndex[f.toLowerCase()] = path.join(imagesDir, f);
    }
  }
} catch {
  imageIndex = Object.create(null);
}

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
  const name = req.query.file;
  if (!name || typeof name !== "string") return res.status(400).send("Missing file parameter.");
  if (!/^[A-Za-z0-9_.-]+$/.test(name)) return res.status(400).send("Invalid file name.");
  const filePath = imageIndex[name.toLowerCase()];
  if (!filePath) return res.status(404).send("File not found.");
  res.set("Cache-Control", "public, max-age=86400");
  res.sendFile(filePath, (err) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).send("File not found.");
      return res.status(500).send("Error serving file.");
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
