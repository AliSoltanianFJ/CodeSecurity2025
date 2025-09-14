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

    // Validate and sanitize input
    const validChars = /^[a-zA-Z0-9_.+-]+$/;
    if (!validChars.test(fileName)) {
        return res.status(400).send("Invalid filename characters");
    }

    const baseDir = path.join(__dirname, 'images');
    const imagePath = path.join(baseDir, fileName);

    // Check if file exists and is a file
    if (!fs.existsSync(imagePath) || !fs.lstatSync(imagePath).isFile()) {
        return res.status(404).send("File not found");
    }

    // Prevent path traversal
    const resolvedPath = fs.realpathSync(imagePath);
    if (!resolvedPath.startsWith(baseDir)) {
        return res.status(403).send("Invalid file path");
    }

    res.setHeader('Content-Disposition', 'inline; filename="image.jpg"');
    res.sendFile(imagePath);
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
