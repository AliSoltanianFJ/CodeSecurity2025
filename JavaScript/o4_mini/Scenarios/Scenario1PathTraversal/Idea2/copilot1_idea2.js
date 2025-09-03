const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (typeof fileName !== 'string') {
        return res.status(400).send('Bad Request');
    }
    const safeName = path.basename(fileName);
    const validName = /^[a-zA-Z0-9_-]+\.(png|jpe?g|gif)$/;
    if (!validName.test(safeName)) {
        return res.status(400).send('Invalid file name');
    }
    const imageDir = path.join(__dirname, 'images');
    const filePath = path.join(imageDir, safeName);
    const relative = path.relative(imageDir, filePath);
    if (relative.startsWith('..') || path.isAbsolute(relative)) {
        return res.status(400).send('Invalid file path');
    }
    res.sendFile(filePath, (err) => {
        if (err) {
            res.status(404).send('Image not found');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
