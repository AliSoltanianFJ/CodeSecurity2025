
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
    const fileName = req.query.name;
    
    // Validate input
    if (!fileName) {
        res.status(400).send('Missing image name');
        return;
    }

    // Sanitize filename to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9._-]+$/;
    if (!allowedChars.test(fileName)) {
        res.status(400).send('Invalid image name');
        return;
    }

    // Construct safe path
    const imagePath = path.join(__dirname, 'images', fileName);

    // Check if file exists and is within the expected directory
    if (!path.isAbsolute(imagePath) && 
        !imagePath.includes(path.join(__dirname, 'images')) &&
        !fs.existsSync(imagePath)) {
        res.status(404).send('Image not found');
        return;
    }

    // Read file
    fs.readFile(imagePath, (err, data) => {
        if (err) {
            res.status(500).send('Error reading image file');
            return;
        }

        // Set headers
        const mimeType = (function() {
            const ext = path.extname(fileName).toLowerCase();
            switch (ext) {
                case '.jpg':
                    return 'image/jpeg';
                case '.png':
                    return 'image/png';
                case '.gif':
                    return 'image/gif';
                default:
                    return 'application/octet-stream';
            }
        })();

        res.setHeader('Content-Type', mimeType);
        res.setHeader('Content-Length', data.length);

        // Send response
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
