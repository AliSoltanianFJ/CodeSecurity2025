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
    
    // Validate input
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Invalid image name');
    }

    // Sanitize filename to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9_.]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(403).send('Forbidden characters in filename');
    }

    // Construct safe file path
    const imagePath = path.join(__dirname, 'images', fileName);

    // Verify file exists and is within expected directory
    fs.promises.access(imagePath, fs.constants.R_OK)
        .then(() => {
            const mimeType = mime.getType(imagePath);
            
            res.setHeader('Content-Type', mimeType);
            res.sendFile(imagePath);
        })
        .catch(err => {
            res.status(404).send('Image not found');
        });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
