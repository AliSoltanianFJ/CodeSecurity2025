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
    const fileName = req.query.filename;
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Validate and sanitize filename to prevent path traversal
    const imagePath = path.join(__dirname, "images", fileName);
    
    // Send the image file
    res.sendFile(imagePath, (err) => {
        if (err) {
            console.error("Error serving image:", err);
            return res.status(404).send("Image not found");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
