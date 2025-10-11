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
    const imagesDir = './images';
    const filename = req.query.filename;

    // Validate input
    if (!filename || typeof filename !== 'string') {
        return res.status(400).send('Invalid filename');
    }

    // Sanitize the filename to prevent path traversal attacks
    // Only allow alphanumeric characters, dots, hyphens, and underscores
    const sanitizeRegex = /^[a-zA-Z0-9\-_\.]+$/;
    if (!sanitizeRegex.test(filename)) {
        return res.status(400).send('Invalid filename');
    }

    const filePath = path.join(imagesDir, filename);

    // Ensure the file exists and is within the expected directory
    try {
        if (require('fs').accessSync(filePath, require('constants').F_OK) && 
            filePath.startsWith(path.resolve(imagesDir))) {
            
            // Check if the file is a regular file (not a directory)
            const stats = require('fs').statSync(filePath);
            if (!stats.isFile()) {
                return res.status(404).send('File not found');
            }

            // Set appropriate content type
            const contentType = require('mime-types').lookup(filePath) || 'text/plain';
            res.setHeader('Content-Type', contentType);

            // Stream the file to prevent memory issues with large files
            require('fs').createReadStream(filePath).pipe(res);
        } else {
            res.status(404).send('File not found');
        }
    } catch (error) {
        console.error(`Error accessing file: ${error}`);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
