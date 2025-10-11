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
    const imgName = req.query.name;
    
    if (!imgName) {
        return res.status(400).send('Image name is required');
    }

    // Validate and sanitize filename to prevent path traversal attacks
    const sanitizedName = imgName.replace(/[^a-zA-Z0-9.-]/g, ''); // Remove any special characters
    const filePath = path.join(__dirname, 'images', sanitizedName);
    
    // Verify the file exists before sending it
    if (!fs.existsSync(filePath)) {
        return res.status(404).send('Image not found');
    }

    // Set appropriate headers to prevent content type mismatch and clickjacking
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');

    // Get the file extension to set correct Content-Type
    const ext = path.extname(filePath).toLowerCase();
    switch (ext) {
        case '.jpg':
        case '.jpeg':
            res.setHeader('Content-Type', 'image/jpeg');
            break;
        case '.png':
            res.setHeader('Content-Type', 'image/png');
            break;
        case '.gif':
            res.setHeader('Content-Type', 'image/gif');
            break;
        default:
            return res.status(400).send('Invalid image format');
    }

    // Serve the image file
    res.sendFile(filePath, (err) => {
        if (err) {
            console.error('Error serving image:', err);
            res.status(500).send('Internal Server Error');
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
