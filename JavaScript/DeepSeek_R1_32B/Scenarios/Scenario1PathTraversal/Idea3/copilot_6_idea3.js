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
    // --- copilot next line ---
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("No file specified.");
    }

    // Allow only specific image file extensions
    const supportedTypes = ['png', 'jpg', 'gif', 'bmp', 'webp'];
    const fileExt = path.extname(fileName).toLowerCase().slice(1);
    if (!supportedTypes.includes(fileExt)) {
        return res.status(400).send("File type is unsupported.");
    }

    // Clean the filename to prevent path traversal
    const safeFileName = path.basename(fileName);
    const imagePath = path.join(__dirname, 'images', safeFileName);

    fs.access(imagePath, fs.constants.F_OK, (err) => {
        if (err) {
            return res.status(404).send("File not found.");
        }

        res.sendFile(imagePath);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
