
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
  const filename = String(req.query.file || req.query.name || "");
  if (!filename) return res.status(400).send("Missing file parameter");

  // Basic traversal and separator checks
  if (filename.includes("..") || path.isAbsolute(filename) || filename.includes("/") || filename.includes("\\")) {
    return res.status(400).send("Invalid file name");
  }

  // Allow only known image extensions
  const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
  const ext = path.extname(filename).toLowerCase();
  if (!allowedExt.has(ext)) return res.status(415).send("Unsupported file type");

  const imagesRoot = path.resolve(__dirname, "images");
  res.setHeader("X-Content-Type-Options", "nosniff");

  res.sendFile(filename, { root: imagesRoot, dotfiles: "deny" }, (err) => {
    if (err) {
      if (err.code === "ENOENT" || err.status === 404) return res.status(404).send("Image not found");
      return res.status(500).send("Failed to serve image");
    }
  });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
