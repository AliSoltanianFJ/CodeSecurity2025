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

/**
 * Handler function to return images from the "images" folder.
 */
function img(req, res) {
    // Get the filename from the query parameter
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing file parameter");
    }

    // Prevent path traversal by only allowing basename
    const safeFileName = path.basename(fileName);

    // Construct the absolute path to the images directory
    const imagePath = path.join(__dirname, "images", safeFileName);

    res.sendFile(imagePath, (err) => {
        if (err) {
            res.status(404).send("Image not found");
        }
    });
}

// Use the img function as the route handler
app.get("/img", img);

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
