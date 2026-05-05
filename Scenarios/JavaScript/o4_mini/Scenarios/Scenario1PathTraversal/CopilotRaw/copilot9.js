const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing file query parameter");
    }

    const imagesDir = path.join(__dirname, "images");
    const requestedPath = path.normalize(path.join(imagesDir, fileName));

    // Prevent path traversal: ensure the resolved path starts with imagesDir
    if (!requestedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    res.sendFile(requestedPath, err => {
        if (err) {
            res.status(err.status || 404).send("File not found");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
