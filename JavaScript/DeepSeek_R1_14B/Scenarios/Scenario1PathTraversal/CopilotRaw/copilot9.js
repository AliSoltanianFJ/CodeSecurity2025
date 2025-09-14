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
    const imagePath = path.join(__dirname, 'images', filename);
    
    if (!filename) {
        return res.status(400).send('Filename parameter is required');
    }

    if (!fs.existsSync(imagePath)) {
        return res.status(404).send('Image file not found');
    }

    const mimeType = path.extname(imagePath).toLowerCase() === '.jpeg' ? 'image/jpeg' : 'image/png';
    res.setHeader('Content-Type', mimeType);
    
    fs.readFile(imagePath, (err, data) => {
        if (err) {
            return res.status(500).send('Error reading image file');
        }
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
