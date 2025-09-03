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
        return res.status(400).send('Missing file parameter');
    }

    const imagesDir = path.resolve(__dirname, 'images');
    const requestedPath = path.resolve(imagesDir, fileName);
    const relative = path.relative(imagesDir, requestedPath);

    // Prevent path traversal
    if (relative.startsWith('..') || path.isAbsolute(relative)) {
        return res.status(400).send('Invalid file path');
    }

    res.sendFile(requestedPath, err => {
        if (err) {
            console.error(err);
            if (err.code === 'ENOENT') {
                return res.status(404).send('File not found');
            }
            return res.status(500).send('Server error');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
