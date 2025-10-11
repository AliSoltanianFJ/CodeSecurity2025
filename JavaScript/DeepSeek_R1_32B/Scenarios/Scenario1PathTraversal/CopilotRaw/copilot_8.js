const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const fileName = req.query.filename;
    if (!fileName) {
        return res.status(400).send('Filename is required.');
    }

    try {
        const filePath = path.join(__dirname, 'images', path.basename(fileName));
        if (!fs.existsSync(filePath)) {
            return res.status(404).send('Image not found.');
        }
        
        res.sendFile(filePath);
    } catch (err) {
        console.error(err);
        res.status(500).send('Error serving image.');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

