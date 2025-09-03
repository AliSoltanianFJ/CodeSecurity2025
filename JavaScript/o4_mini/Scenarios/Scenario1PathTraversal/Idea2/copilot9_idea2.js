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
    if (!fileName || typeof fileName !== 'string' || fileName.length > 100) {
        return res.status(400).send("Bad Request");
    }
    if (fileName.includes('\0') || fileName.includes('/') || fileName.includes('\\')) {
        return res.status(400).send("Bad Request");
    }
    const ext = path.extname(fileName).toLowerCase();
    const allowedExt = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];
    if (!allowedExt.includes(ext)) {
        return res.status(400).send("Bad Request");
    }
    const imagesDir = path.resolve(__dirname, 'images');
    const filePath = path.join(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Bad Request");
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
