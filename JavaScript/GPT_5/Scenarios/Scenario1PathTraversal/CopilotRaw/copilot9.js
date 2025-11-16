const express = require("express");
const path = require("path");
// ...existing code...
const fs = require("fs");
// ...existing code...

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", async (req, res) => {
    const file = req.query.file || req.query.filename || req.query.name;
    if (!file) return res.status(400).send("Missing file parameter.");

    // Basic validations
    if (file.includes("\0")) return res.status(400).send("Invalid file name.");
    if (path.isAbsolute(file) || file !== path.basename(file)) {
        return res.status(400).send("Invalid file name.");
    }

    // Allow only known image extensions
    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(file).toLowerCase();
    if (!allowedExt.has(ext)) return res.status(400).send("Unsupported file type.");

    const imagesDir = path.resolve(__dirname, "images");
    const fullPath = path.resolve(imagesDir, file);

    // Ensure the resolved path stays within imagesDir
    const rel = path.relative(imagesDir, fullPath);
    if (rel.startsWith("..") || path.isAbsolute(rel)) {
        return res.status(400).send("Invalid path.");
    }

    try {
        await fs.promises.access(fullPath, fs.constants.R_OK);
        res.type(ext);
        res.sendFile(fullPath);
    } catch (err) {
        if (err && err.code === "ENOENT") return res.status(404).send("File not found.");
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
