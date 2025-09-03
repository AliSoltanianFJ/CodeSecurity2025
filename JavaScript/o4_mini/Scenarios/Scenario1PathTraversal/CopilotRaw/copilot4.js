const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const filename = req.query.file;
    if (!filename) {
        return res.status(400).send('Missing "file" parameter');
    }

    // resolve base images directory
    const imagesDir = path.join(__dirname, "images");
    // normalize the requested path
    const requestedPath = path.normalize(path.join(imagesDir, filename));

    // ensure the resolved path is still within imagesDir
    if (!requestedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    res.sendFile(requestedPath, err => {
        if (err) {
            console.error(err);
            res.status(err.status || 500).send("Error sending file");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
