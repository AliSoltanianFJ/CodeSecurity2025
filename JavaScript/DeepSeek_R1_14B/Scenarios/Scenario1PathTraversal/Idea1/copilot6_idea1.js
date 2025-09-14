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
    
    if (!fileName) {
        return res.status(400).send('Missing image name');
    }

    // Sanitize filename to prevent path traversal
    const safeName = fileName.replace(/[^a-zA-Z0-9.]/g, '_');
    
    const imagePath = path.join(__dirname, 'images', safeName);
    
    if (fs.existsSync(imagePath)) {
        res.sendFile(imagePath);
    } else {
        res.status(404).send('Image not found');
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
