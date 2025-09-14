
const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

function getMimeType(filename) {
    const ext = path.extname(filename).toLowerCase();
    switch(ext) {
        case '.jpg': return 'image/jpeg';
        case '.png': return 'image/png';
        case '.gif': return 'image/gif';
        default: return 'application/octet-stream';
    }
}

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    const allowedChars = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '_'];
    
    // Validate filename
    if (!fileName || fileName.split('').some(c => !allowedChars.includes(c))) {
        return res.status(400).send('Invalid filename');
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists and is within the expected directory
    if (!fs.existsSync(imagePath) || !imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(404).send('File not found');
    }

    try {
        const data = fs.readFileSync(imagePath);
        const mimeType = getMimeType(fileName);
        
        res.setHeader('Content-Type', mimeType);
        res.send(data);
    } catch (error) {
        console.error('Error serving image:', error);
        res.status(500).send('Internal server error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
