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
    const imagesDir = path.resolve(__dirname, "images");
    const name = req.query.file;
    const IMAGE_NAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}\.(?:png|jpe?g|gif|webp|svg)$/i;

    if (typeof name !== "string" || !IMAGE_NAME_PATTERN.test(name)) {
        return res.status(400).send("Invalid or missing file parameter.");
    }

    const fullPath = path.join(imagesDir, name);

    if (!fullPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid path.");
    }

    fs.access(fullPath, fs.constants.R_OK, (err) => {
        if (err) {
            return err.code === "ENOENT"
                ? res.status(404).send("Not found.")
                : res.status(500).send("Error.");
        }
        res.sendFile(fullPath);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
