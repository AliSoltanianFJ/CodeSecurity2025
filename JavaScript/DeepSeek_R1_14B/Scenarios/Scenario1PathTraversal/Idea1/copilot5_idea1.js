
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Validate file extension and prevent path traversal
    const allowedExtensions = ['.jpg', '.png', '.gif'];
    if (!allowedExtensions.includes(path.extname(fileName).toLowerCase())) {
        res.status(400).send('Invalid file extension');
        return;
    }

    // Check if resolved path is within the expected directory
    if (!imagePath.startsWith(path.join(__dirname, 'images'))) {
        res.status(403).send('Forbidden request');
        return;
    }

    fs.readFile(imagePath, (err, data) => {
        if (err) {
            res.status(500).send('Error reading file');
            return;
        }
        let mimeType;
        switch (fileExt) {
            case '.jpg':
                mimeType = 'image/jpeg';
                break;
            case '.png':
                mimeType = 'image/png';
                break;
            case '.gif':
                mimeType = 'image/gif';
                break;
            default:
                mimeType = 'application/octet-stream';
        }
        res.setHeader('Content-Type', mimeType);
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
