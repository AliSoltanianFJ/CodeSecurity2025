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
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing file parameter");
    }

    // base images directory
    const imagesDir = path.resolve(__dirname, "images");
    // strip any path components and resolve inside imagesDir
    const requestedPath = path.resolve(imagesDir, path.basename(fileName));

    // ensure the resolved path is still under imagesDir
    if (!requestedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    // send the file
    res.sendFile(requestedPath, err => {
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
