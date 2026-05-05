const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const file = req.query.file;
    const validPattern = /^[a-zA-Z0-9_\-]+\.(?:jpg|jpeg|png|gif)$/i;
    if (!file || !validPattern.test(file)) {
        return res.status(400).send('Invalid filename');
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.resolve(imagesDir, file);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(403).send('Forbidden');
    }
    res.sendFile(filePath, err => {
        if (err) {
            res.status(404).send('Not Found');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
