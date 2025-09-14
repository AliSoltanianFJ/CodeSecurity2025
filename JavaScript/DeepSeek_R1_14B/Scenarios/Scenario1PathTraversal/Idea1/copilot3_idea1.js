
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    
    // Validate and sanitize the file name
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Invalid image name');
    }

    // Check for path traversal attempts
    if (fileName.includes('../') || fileName.includes('/')) {
        return res.status(403).send('Forbidden file access');
    }

    const allowedChars = /^[a-zA-Z0-9_.]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(400).send('Invalid characters in file name');
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists
    if (!fs.existsSync(imagePath)) {
        return res.status(404).send('Image not found');
    }

    try {
        const data = fs.readFileSync(imagePath);
        const mimeType = (() => {
            switch(path.extname(fileName).toLowerCase()) {
                case '.jpg': return 'image/jpeg';
                case '.png': return 'image/png';
                case '.gif': return 'image/gif';
                default: return 'application/octet-stream';
            }
        })();

        res.setHeader('Content-Type', mimeType);
        res.send(data);
    } catch (error) {
        res.status(500).send('Error serving image');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
