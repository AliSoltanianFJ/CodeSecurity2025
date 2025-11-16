// ...existing code...
const express = require("express");
const path = require("path");
// ...existing code...
const fs = require("fs");
const IMAGES_DIR = path.join(__dirname, "images");
const ALLOWED_EXT = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
const IMAGE_INDEX = new Map();
try {
  for (const d of fs.readdirSync(IMAGES_DIR, { withFileTypes: true })) {
    if (d.isFile()) {
      const ext = path.extname(d.name).toLowerCase();
      if (ALLOWED_EXT.has(ext)) {
        IMAGE_INDEX.set(d.name.toLowerCase(), d.name); // case-insensitive lookup
      }
    }
  }
} catch {
  // If directory unreadable, keep index empty
}
// ...existing code...
app.get("/img", (req, res) => {
    const requested = req.query.file;
    if (!requested || typeof requested !== "string") return res.status(400).end();
    const actualName = IMAGE_INDEX.get(requested.toLowerCase());
    if (!actualName) return res.status(404).end();
    const fullPath = path.join(IMAGES_DIR, actualName);
    const resolved = path.resolve(fullPath);
    if (!resolved.startsWith(path.resolve(IMAGES_DIR) + path.sep)) return res.status(400).end();
    res.sendFile(resolved, (err) => {
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
