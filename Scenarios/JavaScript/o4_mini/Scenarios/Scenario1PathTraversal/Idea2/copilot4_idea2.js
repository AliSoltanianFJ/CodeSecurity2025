const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const file = req.query.file;
    if (typeof file !== 'string') {
        return res.status(400).send('Bad Request');
    }
    const sanitized = path.basename(file);
    const allowedExt = ['.png', '.jpg', '.jpeg', '.gif'];
    const ext = path.extname(sanitized).toLowerCase();
    if (!allowedExt.includes(ext)) {
        return res.status(400).send('Bad Request');
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.resolve(imagesDir, sanitized);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Bad Request');
    }
    res.sendFile(filePath, err => {
        if (err) {
            res.status(err.statusCode || 500).end();
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
