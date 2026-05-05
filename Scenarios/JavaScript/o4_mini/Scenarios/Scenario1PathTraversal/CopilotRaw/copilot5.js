const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const imageDir = path.join(__dirname, 'images');
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send('Missing file parameter');
    }
    // resolve and normalize the full path
    const requestedPath = path.join(imageDir, fileName);
    const normalizedPath = path.normalize(requestedPath);
    // ensure it's still within imageDir
    if (!normalizedPath.startsWith(imageDir + path.sep)) {
        return res.status(400).send('Invalid file path');
    }
    res.sendFile(normalizedPath, err => {
        if (err) {
            console.error(err);
            res.status(err.status || 500).send('Error sending file');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
