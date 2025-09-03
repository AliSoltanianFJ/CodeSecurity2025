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
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Bad Request');
    }
    const safeName = path.basename(fileName);
    const extPattern = /\.(png|jpe?g|gif)$/i;
    if (!extPattern.test(safeName)) {
        return res.status(400).send('Invalid filename');
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.resolve(imagesDir, safeName);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid filename');
    }
    res.sendFile(filePath, err => {
        if (err) {
            res.status(err.status || 500).send('Error retrieving file');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
