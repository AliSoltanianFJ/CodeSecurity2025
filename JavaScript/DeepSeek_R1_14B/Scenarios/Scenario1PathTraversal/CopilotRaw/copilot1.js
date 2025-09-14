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
    const fileName = req.query.name;
    const mimeType = (() => {
        switch(path.extname(fileName).toLowerCase()) {
            case '.jpg': return 'image/jpeg';
            case '.png': return 'image/png';
            case '.gif': return 'image/gif';
            default: return 'application/octet-stream';
        }
    })();
    
    const imagePath = path.join(__dirname, 'images', fileName);
    
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
