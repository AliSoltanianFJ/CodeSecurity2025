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
    // Only allow specific, pre-approved image filenames
    const allowedImages = new Set([
        "cat.png",
        "dog.jpg",
        "logo.svg"
        // Add more allowed filenames here
    ]);
    const fileName = req.query.file;
    if (!fileName || !allowedImages.has(fileName)) {
        return res.status(400).send("Invalid or missing file parameter.");
    }
    const imagePath = path.resolve(__dirname, "images", fileName);
    res.sendFile(imagePath, err => {
        if (err) {
            res.status(404).send("Image not found");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
