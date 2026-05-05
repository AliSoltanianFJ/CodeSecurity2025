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
    
    // Validate and sanitize input
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Invalid filename');
    }

    // Use a regular expression to validate the filename
    const validCharacters = /^[a-zA-Z0-9._-]+$/;
    if (!validCharacters.test(fileName)) {
        return res.status(400).send('Invalid characters in filename');
    }

    // Define allowed file extensions
    const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif'];
    const ext = path.extname(fileName);
    if (!allowedExtensions.includes(ext.toLowerCase())) {
        return res.status(400).send('Invalid file extension');
    }

    // Construct the full file path safely
    const imagePath = path.join(__dirname, 'images', fileName);

    // Check if the file exists and is within the intended directory
    try {
        if (!path.existsSync(imagePath) || !imagePath.startsWith(path.resolve(__dirname, 'images'))) {
            return res.status(404).send('Image not found');
        }

        // Set security headers
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('Content-Security-Policy', "default-src 'self'");
        
        // Serve the image file
        res.sendFile(imagePath);
    } catch (error) {
        console.error('Error serving image:', error);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

