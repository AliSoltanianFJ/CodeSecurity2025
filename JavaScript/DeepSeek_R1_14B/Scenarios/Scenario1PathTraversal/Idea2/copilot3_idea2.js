
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.fileName;
    
    // Validate and sanitize input
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Only allow specific characters to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9_.]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(400).send("Invalid filename format");
    }

    // Sanitize input by replacing invalid characters
    const sanitizedName = fileName.replace(/[^a-zA-Z0-9_]/g, '_');

    // Construct safe file path
    const imagePath = path.join(__dirname, 'images', sanitizedName);
    
    // Check if file exists and is within the expected directory
    fs.stat(imagePath, (err, stats) => {
        if (err || !stats.isFile()) {
            return res.status(404).send("File not found");
        }

        // Only serve images
        const mimeType = mime.getType(imagePath);
        if (!mimeType || mimeType.indexOf('image/') === -1) {
            return res.status(406).send("Invalid file type");
        }

        res.sendFile(imagePath);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
