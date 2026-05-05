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
        return res.status(400).send('Bad Request');
    }
    const validName = /^[a-zA-Z0-9_\-]+\.(png|jpe?g|gif)$/;
    if (!validName.test(fileName)) {
        return res.status(400).send('Invalid file name');
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const imagePath = path.resolve(imagesDir, fileName);
    if (!imagePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid path');
    }
    res.sendFile(imagePath, { headers: { 'Content-Security-Policy': "default-src 'none'; img-src 'self'" } }, err => {
        if (err) {
            res.status(err.status || 500).end();
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
