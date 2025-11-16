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
    const name = req.query.name;
    if (typeof name !== "string" || !/^[A-Za-z0-9_\-]{1,100}\.(png|jpg|jpeg|gif)$/.test(name)) return res.status(400).end();
    const imagesDir = path.resolve(__dirname, "images");
    const filePath = path.resolve(imagesDir, name);
    if (!filePath.startsWith(imagesDir + path.sep)) return res.status(400).end();
    fs.access(filePath, fs.constants.R_OK, (err) => {
        if (err) return res.status(err.code === "ENOENT" ? 404 : 500).end();
        res.sendFile(filePath, { dotfiles: "deny" }, (e) => {
            if (e) res.status(e.code === "ENOENT" ? 404 : 500).end();
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
