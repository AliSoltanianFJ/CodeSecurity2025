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

    if (typeof name !== "string" || !name) return res.status(400).send("Bad request");

    // Allow only safe characters and reasonable length
    if (name.length > 100 || /[^A-Za-z0-9_.-]/.test(name)) {
        return res.status(400).send("Invalid name");
    }

    // Enforce extension allow-list
    const ext = path.extname(name).toLowerCase();
    if (!/^\.(png|jpe?g|gif|webp|svg)$/.test(ext)) {
        return res.status(400).send("Unsupported type");
    }

    // Build absolute path and ensure containment
    const filePath = path.join(imagesDir, name);
    const resolved = path.resolve(filePath);
    if (!resolved.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid path");
    }

    // Check existence & that it's a regular file
    let stat;
    try {
        stat = fs.statSync(resolved);
    } catch {
        return res.status(404).send("Not found");
    }
    if (!stat.isFile()) return res.status(404).send("Not found");

    res.sendFile(resolved, { dotfiles: "deny" }, (err) => {
        if (err) {
            if (err.code === "ENOENT") return res.status(404).send("Not found");
            return res.status(500).send("Error");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
