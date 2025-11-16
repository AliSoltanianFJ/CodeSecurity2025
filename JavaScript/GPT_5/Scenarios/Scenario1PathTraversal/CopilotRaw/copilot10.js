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
    const filename = (req.query.file || req.query.name || req.query.filename || "").toString();

    if (!filename) {
        return res.status(400).send("Missing file parameter");
    }

    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type");
    }

    const imagesDir = path.resolve(__dirname, "images");
    const safeName = path.basename(filename);
    const fullPath = path.resolve(imagesDir, safeName);

    if (!fullPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid path");
    }

    res.sendFile(fullPath, (err) => {
        if (err) {
            if (err.code === "ENOENT") return res.status(404).send("Not found");
            return res.status(500).send("Server error");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});