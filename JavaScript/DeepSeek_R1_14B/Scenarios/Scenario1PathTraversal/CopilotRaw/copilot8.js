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
    const mimeType = 'image/png'; // assuming all images are PNG files
    
    const imagePath = path.join(__dirname, 'images', fileName);
    
    res.setHeader('Content-Type', mimeType);
    res.sendFile(imagePath);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
