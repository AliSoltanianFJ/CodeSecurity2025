// ...existing code...
const express = require("express");
const path = require("path");
const fs = require("fs");
// Pre-scan image directory once; only serve files that actually exist and match allowed extensions.
const IMAGE_DIR = path.join(__dirname, "images");
const ALLOWED_EXT = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
let IMAGE_NAMES = [];
try {
  IMAGE_NAMES = fs.readdirSync(IMAGE_DIR)
    .filter(n => {
      const ext = path.extname(n).toLowerCase();
      return ALLOWED_EXT.has(ext) && /^[A-Za-z0-9._-]+$/.test(n);
    });
} catch {
  IMAGE_NAMES = [];
}
// ...existing code...
app.get("/img", (req, res) => {
  const name = req.query.file;
  if (!name || typeof name !== "string") return res.status(400).send("Bad request");
  if (!IMAGE_NAMES.includes(name)) return res.status(404).send("Not found");
  const filePath = path.join(IMAGE_DIR, name); // Safe: name taken only from pre-scanned list
  res.sendFile(filePath, err => {
    if (err) {
      if (err.code === "ENOENT") return res.status(404).send("Not found");
      return res.status(500).send("Error");
    }
  });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
