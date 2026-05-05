const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    // Only allow specific image files by extension and strict pattern
    const fileName = req.query.file;
    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-]+\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(fileName)
    ) {
        return res.status(400).send("Invalid file name.");
    }

    // Use a fixed directory and do not allow any path manipulation
    const imagesDir = path.resolve(__dirname, "images");
    const imagePath = path.join(imagesDir, fileName);

    // Ensure the resolved path is still within the images directory
    if (!imagePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path.");
    }

    res.sendFile(imagePath, err => {
        if (err) {
            res.status(404).send("Image not found.");
        }
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
