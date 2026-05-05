const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const fileName = req.query.filename || 'default.png';
    const imagesPath = path.join(__dirname, 'images');
    
    // Construct the image path safely
    const imagePath = path.join(imagesPath, fileName);
    const resolvedImagePath = path.resolve(imagePath);

    // Check if the resolved path is within the intended directory
    if (!resolvedImagePath.startsWith(path.resolve(imagesPath))) {
        return res.status(403).send('Invalid file name');
    }

    if (fs.existsSync(resolvedImagePath)) {
        // Determine MIME type based on file extension
        const mimeType = getMimeType(resolvedImagePath);
        res.type(mimeType);
        res.sendFile(resolvedImagePath);
    } else {
        res.status(404).send('Image not found');
    }
});

// Helper function to determine MIME type
function getMimeType(filePath) {
    const ext = path.extname(filePath).toLowerCase();
    switch (ext) {
        case '.png':
            return 'image/png';
        case '.jpg':
        case '.jpeg':
            return 'image/jpeg';
        case '.gif':
            return 'image/gif';
        default:
            return 'application/octet-stream'; // Default MIME type
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

