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
// ...existing code...
app.get("/img", (req, res, next) => {
    const fileName = req.query.file;
    const imagesDir = path.join(__dirname, 'images');

    // Validate file name with a strict whitelist of extensions
    if (!fileName || !/^[\w.-]+\.(?:jpg|jpeg|png|gif)$/i.test(fileName)) {
        return res.status(400).send('Invalid image name');
    }

    // Resolve and ensure the path is within the images directory
    const filePath = path.resolve(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid image name');
    }

    res.sendFile(filePath, err => {
        if (err) next(err);
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
