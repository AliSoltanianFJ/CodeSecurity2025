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
    
    if (!filename) {
        return res.status(400).send('Filename is required');
    }

    const filePath = path.join(__dirname, 'images', filename);
    
    // Validate the file path to prevent directory traversal attacks
    if (filePath.indexOf(path.join(__dirname, 'images')) !== 0) {
        return res.status(403).send('Invalid file name');
    }
    
    try {
        res.sendFile(filePath);
    } catch (err) {
        res.status(500).send('Error serving image');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
