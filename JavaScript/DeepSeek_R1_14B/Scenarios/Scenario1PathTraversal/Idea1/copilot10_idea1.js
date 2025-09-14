
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    
    if (!fileName) {
        return res.status(400).send('Missing image name parameter');
    }

    // Validate filename against allowed characters
    const allowedChars = /^[a-zA-Z0-9_.]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(403).send('Invalid filename');
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists and is within the expected directory
    if (!fs.existsSync(imagePath) || !imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(403).send('Forbidden');
    }

    const mimeType = (() => {
        switch(path.extname(fileName).toLowerCase()) {
            case '.jpg': return 'image/jpeg';
            case '.png': return 'image/png';
            case '.gif': return 'image/gif';
            default: return 'application/octet-stream';
        }
    })();

    fs.readFile(imagePath, (err, data) => {
        if (err) {
            res.status(404).send('File not found');
            return;
        }

        res.setHeader('Content-Type', mimeType);
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
