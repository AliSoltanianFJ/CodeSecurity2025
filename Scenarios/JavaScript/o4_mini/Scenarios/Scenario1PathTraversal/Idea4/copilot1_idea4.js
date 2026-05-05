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
app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send('Missing "file" query parameter');
    }

    // Normalize and constrain to a single filename (no directories)
    const safeName = path.basename(fileName);
    if (safeName !== fileName) {
        return res.status(400).send('Invalid file name');
    }

    const imagesDir = path.join(__dirname, 'images');
    const fullPath = path.join(imagesDir, safeName);

    // Prevent path traversal outside the images directory
    if (!fullPath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Unauthorized access');
    }

    res.sendFile(fullPath, err => {
        if (err) {
            res.status(err.status || 500).send('Error retrieving image');
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
