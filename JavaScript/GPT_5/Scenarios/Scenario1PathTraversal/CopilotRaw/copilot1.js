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
    const imagesDir = path.join(__dirname, "images");
    const filename = req.query.file;

    if (!filename || typeof filename !== "string") {
        return res.status(400).send("Missing 'file' query parameter.");
    }

    // Reject attempts at path traversal or absolute paths
    if (
        filename.includes("..") ||
        path.isAbsolute(filename) ||
        filename.includes("/") ||
        filename.includes("\\")
    ) {
        return res.status(400).send("Invalid file name.");
    }

    const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg"]);
    const ext = path.extname(filename).toLowerCase();
    if (!allowedExt.has(ext)) {
        return res.status(400).send("Unsupported file type.");
    }

    res.sendFile(filename, { root: imagesDir }, (err) => {
        if (err) {
            if (err.code === "ENOENT") return res.status(404).send("File not found.");
            if (err.status) return res.status(err.status).send("Error serving file.");
            return res.status(500).send("Server error.");
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
