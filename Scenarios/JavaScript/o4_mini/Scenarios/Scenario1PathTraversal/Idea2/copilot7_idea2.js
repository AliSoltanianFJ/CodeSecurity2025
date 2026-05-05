const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const fileName = req.query.name;
    if (typeof fileName !== 'string') {
        return res.status(400).send('Bad Request');
    }
    if (!/^[a-zA-Z0-9_-]+\.(?:jpg|jpeg|png|gif)$/i.test(fileName)) {
        return res.status(400).send('Invalid file name');
    }
    const imagesDir = path.join(__dirname, 'images');
    const requestedPath = path.normalize(path.join(imagesDir, fileName));
    if (!requestedPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid file path');
    }
    res.sendFile(requestedPath, err => {
        if (err) {
            return res.status(err.status || 500).end();
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
