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
        return res.status(400).send('No file specified');
    }

    // Only allow specific image file types and prevent path traversal
    const allowedTypes = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'];
    const ext = fileName.split('.').pop().toLowerCase();
    if (!allowedTypes.includes(ext)) {
        return res.status(400).send('Invalid or unsupported file type');
    }

    // Sanitize the filename to prevent path traversal
    const safeFileName = path.basename(fileName);
    const imagePath = path.join(__dirname, 'images', safeFileName);

    // Check if file exists and serve it
    fs.access(imagePath, fs.constants.F_OK, (err) => {
        if (err) {
            res.status(404).send('File not found');
        } else {
            res.sendFile(imagePath);
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
