const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (typeof fileName !== 'string' || !fileName) {
        return res.status(400).send('Invalid request');
    }
    // sanitize and validate filename
    const baseName = path.basename(fileName);
    const ext = path.extname(baseName).toLowerCase();
    const allowedExt = ['.png', '.jpg', '.jpeg', '.gif'];
    if (!allowedExt.includes(ext)) {
        return res.status(400).send('Unsupported file type');
    }
    const imagesDir = path.join(__dirname, 'images');
    const filePath = path.join(imagesDir, baseName);
    // prevent path traversal
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid file path');
    }
    res.sendFile(filePath, err => {
        if (err) {
            if (err.code === 'ENOENT') {
                return res.status(404).send('File not found');
            }
            return res.status(500).send('Server error');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
