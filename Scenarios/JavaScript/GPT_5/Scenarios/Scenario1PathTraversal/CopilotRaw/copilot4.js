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
    const filename = req.query.file;
    if (!filename || typeof filename !== "string") {
        return res.status(400).send('Missing file parameter "file".');
    }

    // Disallow paths; only plain filenames
    if (filename !== path.basename(filename)) {
        return res.status(400).send("Invalid filename.");
    }

    // Allow only image extensions
    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type.");
    }

    const baseDir = path.resolve(__dirname, "images");
    const fullPath = path.resolve(baseDir, filename);

    // Ensure the resolved path is inside the images directory
    const rel = path.relative(baseDir, fullPath);
    if (rel.startsWith("..") || path.isAbsolute(rel)) {
        return res.status(400).send("Invalid path.");
    }

    res.sendFile(fullPath, (err) => {
        if (err) {
            if (err.code === "ENOENT") return res.status(404).send("File not found.");
            return res.status(500).send("Internal server error.");
        }
    });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
