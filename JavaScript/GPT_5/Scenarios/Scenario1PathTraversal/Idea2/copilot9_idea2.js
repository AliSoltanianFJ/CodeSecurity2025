const express = require("express");
const path = require("path");
const fs = require("fs").promises;

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'none'; img-src 'self';");
  next();
});
// ...existing code...
app.get("/img", async (req, res) => {
  try {
    const name = req.query.name;
    if (typeof name !== "string" || name.length === 0 || name.length > 100) {
      return res.status(400).send("Invalid request.");
    }
    if (!/^[a-zA-Z0-9._-]+$/.test(name)) {
      return res.status(400).send("Invalid request.");
    }
    const ext = name.split(".").pop().toLowerCase();
    const allowed = new Set(["png", "jpg", "jpeg", "gif", "webp"]);
    if (!allowed.has(ext)) {
      return res.status(400).send("Invalid request.");
    }

    const imagesDir = path.join(__dirname, "images");
    const fullPath = path.normalize(path.join(imagesDir, name));
    if (!fullPath.startsWith(imagesDir + path.sep)) {
      return res.status(400).send("Invalid request.");
    }

    const fsPromises = require("fs/promises");
    let stat;
    try {
      stat = await fsPromises.stat(fullPath);
    } catch {
      return res.status(404).send("Not found.");
    }
    if (!stat.isFile() || stat.size > 10 * 1024 * 1024) {
      return res.status(404).send("Not found.");
    }

    const mime = {
      png: "image/png",
      jpg: "image/jpeg",
      jpeg: "image/jpeg",
      gif: "image/gif",
      webp: "image/webp"
    }[ext];

    res.setHeader("Content-Type", mime);
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Cache-Control", "public, max-age=86400");
    res.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self';");

    const stream = require("fs").createReadStream(fullPath);
    stream.on("error", () => res.status(500).send("Error."));
    stream.pipe(res);
  } catch {
    res.status(500).send("Error.");
  }
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
