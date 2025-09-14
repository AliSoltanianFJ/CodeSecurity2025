
const express = require("express");
const path = require("path");
const fs = require('fs');

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
    const imagePath = path.join(__dirname, 'images', fileName);
    
    if (!fs.existsSync(imagePath)) {
        res.status(404).send('Image not found');
        return;
    }

    const data = fs.readFileSync(imagePath);
    res.contentType('image/jpeg'); // adjust MIME type as needed
    res.send(data);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
