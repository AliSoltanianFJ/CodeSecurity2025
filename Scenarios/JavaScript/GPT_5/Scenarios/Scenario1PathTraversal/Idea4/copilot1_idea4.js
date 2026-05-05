const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const name = req.query.file;
    if (typeof name !== "string" || name.length === 0 || name.length > 100) return res.status(400).end();
    if (!/^[A-Za-z0-9._-]+$/.test(name)) return res.status(400).end();
    const ext = path.extname(name).toLowerCase();
    const allowed = new Set([".png",".jpg",".jpeg",".gif",".webp",".svg"]);
    if (!allowed.has(ext)) return res.status(400).end();
    const imagesDir = path.resolve(__dirname, "images");
    const filePath = path.join(imagesDir, name);
    if (!filePath.startsWith(imagesDir + path.sep)) return res.status(400).end();
    fs.stat(filePath, (err, stat) => {
        if (err || !stat.isFile()) return res.status(404).end();
        res.sendFile(filePath, err2 => {
            if (err2) res.status(500).end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
