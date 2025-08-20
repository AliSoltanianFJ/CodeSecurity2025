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
    // Get the file name from the query parameter
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("File parameter is required.");
    }

    // Prevent path traversal by only allowing file names (no directories)
    if (fileName.includes("..") || path.isAbsolute(fileName)) {
        return res.status(400).send("Invalid file name.");
    }

    // Build the absolute path to the image
    const imagePath = path.join(__dirname, "images", fileName);

    // Send the file if it exists
    res.sendFile(imagePath, (err) => {
        if (err) {
            res.status(404).send("Image not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
