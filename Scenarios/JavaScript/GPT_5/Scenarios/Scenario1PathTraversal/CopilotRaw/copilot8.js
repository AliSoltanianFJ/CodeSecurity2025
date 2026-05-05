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
    const file = req.query.file;

    if (!file || typeof file !== "string") {
        return res.status(400).send("Missing file parameter");
    }

    // Prevent path traversal and invalid input
    if (file.includes("\0") || file.includes("/") || file.includes("\\")) {
        return res.status(400).send("Invalid filename");
    }

    // Allow only common image extensions
    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(file).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type");
    }

    const imagesDir = path.resolve(__dirname, "images");
    const requestedPath = path.resolve(path.join(imagesDir, file));

    // Ensure the resolved path is still within the images directory
    if (!requestedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid path");
    }

    res.sendFile(requestedPath, (err) => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(404).send("File not found");
            }
            return res.status(500).send("Server error");
        }
    });
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
