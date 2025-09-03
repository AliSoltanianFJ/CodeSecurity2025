const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const imagesDir = path.resolve(__dirname, "images");
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing 'file' query parameter");
    }

    // only keep the basename, reject any attempts to sneak in ../ or absolute paths
    const safeName = path.basename(fileName);
    if (safeName !== fileName) {
        return res.status(400).send("Invalid file path");
    }

    const filePath = path.join(imagesDir, safeName);
    const resolvedPath = path.resolve(filePath);
    if (!resolvedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    res.sendFile(resolvedPath, err => {
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
