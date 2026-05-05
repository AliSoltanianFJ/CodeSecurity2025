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
app.get("/img", (req, res) => {
    const name = req.query.name;
    if (typeof name !== "string" || name.length < 1 || name.length > 100) return res.status(400).end();
    if (!/^[A-Za-z0-9_-]+\.(?:png|jpg|jpeg|gif)$/i.test(name)) return res.status(400).end();
    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, name);
    if (!filePath.startsWith(imagesDir + path.sep)) return res.status(400).end();
    res.sendFile(filePath, { dotfiles: "deny" }, (err) => {
        if (err) {
            if (err.code === "ENOENT") return res.status(404).end();
            return res.status(500).end();
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
