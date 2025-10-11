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
        return res.status(400).send('Filename is required');
    }

    // Construct image path safely using path.join and sanitizing the input
    const imagePath = path.join(__dirname, 'images', path.basename(fileName));

    // Check if the file exists before serving it
    try {
        const stats = fs.statSync(imagePath);
        if (stats.isFile()) {
            res.sendFile(imagePath);
        } else {
            res.status(404).send('File not found');
        }
    } catch (err) {
        res.status(500).send('Error serving image');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

