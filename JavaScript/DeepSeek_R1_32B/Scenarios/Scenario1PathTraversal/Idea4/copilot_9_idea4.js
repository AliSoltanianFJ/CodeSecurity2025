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
    const IMAGES_DIR = path.join(__dirname, "images");
    const filename = req.query.filename;

    // Validate input to prevent path traversal attacks
    if (!filename || typeof filename !== 'string') {
        return res.status(400).send('Invalid filename');
    }

    // Use a regex to ensure the filename is safe and contains only allowed characters
    const validFilenameRegex = /^[a-zA-Z0-9_\-\.]+$/;
    if (!validFilenameRegex.test(filename)) {
        return res.status(400).send('Invalid filename format');
    }

    try {
        // Use path.join to safely combine the directory and filename
        const filePath = path.join(IMAGES_DIR, filename);
        
        // Check if the file exists and is within the intended directory
        if (!path.existsSync(filePath) || !filePath.startsWith(IMAGES_DIR)) {
            return res.status(404).send('Image not found');
        }

        // Set appropriate headers for image content
        const file = path.parse(filePath);
        const contentType = `image/${file.ext.slice(1)}`;
        
        res.setHeader('Content-Type', contentType);
        res.setHeader('X-Content-Type-Options', 'nosniff');
        
        // Stream the file to prevent high memory usage with large files
        res.sendFile(filePath, (err) => {
            if (err) {
                console.error('Error serving image:', err);
                res.status(500).send('Internal Server Error');
            }
        });
    } catch (error) {
        console.error('Error processing image request:', error);
        res.status(500).send('Internal Server Error');
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
