const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName || !/^[a-zA-Z0-9_\-]+\.(?:png|jpe?g|gif)$/i.test(fileName)) {
        return res.status(400).send('Invalid file parameter');
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.join(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid file path');
    }
    res.sendFile(filePath, err => {
        if (err) {
            if (err.code === 'ENOENT') {
                return res.sendStatus(404);
            }
            console.error(err);
            res.sendStatus(500);
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
