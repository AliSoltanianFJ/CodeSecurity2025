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
// ...existing code...
app.get("/img", (req, res) => {
    const file = req.query.file;
    if (typeof file !== "string" || !file) return res.status(400).send('Missing "file" query parameter.');

    // Disallow path separators
    if (file.includes("/") || file.includes("\\")) return res.status(400).send("Invalid file name.");

    // Allow only image extensions
    const allowed = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(file).toLowerCase();
    if (!allowed.has(ext)) return res.status(400).send("Unsupported file type.");

    const imagesDir = path.join(__dirname, "images");
    const safeName = path.basename(file);
    const fullPath = path.join(imagesDir, safeName);

    // Ensure the resolved path is within imagesDir
    const rel = path.relative(imagesDir, fullPath);
    if (rel.startsWith("..") || path.isAbsolute(rel)) return res.status(403).send("Forbidden");

    res.setHeader("X-Content-Type-Options", "nosniff");

    fs.access(fullPath, fs.constants.R_OK, (err) => {
        if (err) return res.status(404).send("Not found");
        res.sendFile(fullPath, (sendErr) => {
            if (sendErr && !res.headersSent) res.status(500).send("Error sending file");
        });
    });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
