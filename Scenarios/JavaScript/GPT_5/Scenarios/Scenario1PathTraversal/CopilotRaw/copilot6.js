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
    // --- copilot next line ---
    const fileParam = (req.query.file || req.query.filename || "").toString();
    if (!fileParam) {
      return res.status(400).send("Missing 'file' query parameter.");
    }

    // Prevent path traversal and restrict to image types
    const safeName = path.basename(fileParam);
    const ext = path.extname(safeName).toLowerCase();
    const allowed = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);
    if (!allowed.has(ext)) {
      return res.status(400).send("Unsupported file type.");
    }

    res.sendFile(
      safeName,
      {
        root: path.join(__dirname, "images"),
        dotfiles: "deny",
        headers: { "Cache-Control": "public, max-age=86400" }
      },
      (err) => {
        if (err) {
          if (err.code === "ENOENT") return res.status(404).send("Image not found.");
          if (err.status) return res.status(err.status).end();
          return res.status(500).send("Internal server error.");
        }
      }
    );
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
