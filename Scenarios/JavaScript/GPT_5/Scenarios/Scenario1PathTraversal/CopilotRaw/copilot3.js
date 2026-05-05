
// ...existing code...
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
// ...existing code...
app.get("/img", (req, res) => {
  const filename = req.query.file || req.query.name || req.query.filename;
  if (!filename || typeof filename !== "string") {
    return res.status(400).send("Missing filename");
  }

  // Ensure it's a plain filename (no directories)
  const safeName = path.basename(filename);
  if (safeName !== filename) {
    return res.status(400).send("Invalid filename");
  }

  // Allow-list extensions
  const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
  const ext = path.extname(safeName).toLowerCase();
  if (!allowedExt.has(ext)) {
    return res.status(400).send("Unsupported file type");
  }

  const imagesDir = path.resolve(__dirname, "images");
  const absPath = path.resolve(imagesDir, safeName);

  // Final containment check
  const rel = path.relative(imagesDir, absPath);
  if (rel.startsWith("..") || path.isAbsolute(rel)) {
    return res.status(400).send("Invalid path");
  }

  res.sendFile(absPath, (err) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).send("File not found");
      return res.status(500).send("Error serving file");
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
