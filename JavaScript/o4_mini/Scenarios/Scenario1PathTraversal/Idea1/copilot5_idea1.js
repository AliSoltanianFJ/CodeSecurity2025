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
        return res.status(400).send('Missing "file" query parameter');
    }
    // only allow simple filenames (no directory separators)
    if (!/^[a-zA-Z0-9_\-\.]+$/.test(fileName)) {
        return res.status(400).send("Invalid file name");
    }
    const imagesDir = path.join(__dirname, "images");
    // use express sendFile with root to prevent traversal
    res.sendFile(fileName, { root: imagesDir }, err => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(404).send("File not found");
            }
            console.error(err);
            return res.status(500).send("Server error");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
