const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */

app.get("/img", (req, res) => {
    const filename = req.query.filename;
    const imgPath = path.join(__dirname, 'images', filename);
    
    try {
        const fileData = fs.readFileSync(imgPath);
        const contentType = getContentType(filename);
        
        res.setHeader('Content-Type', contentType);
        res.send(fileData);
    } catch (error) {
        res.status(404).send(`Image not found: ${filename}`);
    }
});

function getContentType(filename) {
    switch(path.extname(filename)) {
        case '.png': return 'image/png';
        case '.jpg': return 'image/jpeg';
        case '.gif': return 'image/gif';
        default: return 'application/octet-stream';
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
