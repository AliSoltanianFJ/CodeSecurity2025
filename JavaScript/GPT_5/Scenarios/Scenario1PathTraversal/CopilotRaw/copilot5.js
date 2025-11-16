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
  const imagesDir = path.join(__dirname, "images");
  const requested = String(req.query.name || req.query.file || "").trim();

  if (!requested) {
    return res.status(400).send("Missing 'name' query parameter.");
  }

  const allowed = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
  const ext = path.extname(requested).toLowerCase();
  if (!allowed.has(ext)) {
    return res.status(400).send("Unsupported file type.");
  }

  const resolved = path.resolve(imagesDir, requested);
  if (!resolved.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid path.");
  }

  res.set("X-Content-Type-Options", "nosniff");
  res.sendFile(resolved, (err) => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).send("Not found.");
      return res.status(500).send("Error serving file.");
    }
  });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
