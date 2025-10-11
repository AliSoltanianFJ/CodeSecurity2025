const express = require("express");
const path = require("path");
const fs = require("fs");
const { createReadStream } = require("fs");
const { promisify } = require("util");
const mime = require("mime-types");
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * url to return images from the folder "images", file name in GET variable
 */


// Sanitize filename function
function sanitizeFileName(filename) {
    // Remove any characters that might be used for directory traversal or malicious purposes
    const allowedChars = /^[a-zA-Z0-9-.]+$/;
    let cleaned = filename.replace(/[^]/g, '');
    cleaned = cleaned.replace(/\./g, '.');  // Keep dots but prevent multiple extensions
    cleaned = path.posix.basename(cleaned); // Get only the basename to prevent path traversal
    
    if (cleaned !== filename) {
        return null; // If anything was changed during cleaning, consider it invalid
    }
    
    return cleaned;
}

app.get("/img", async (req, res) => {
    try {
        const { name } = req.query;

        // 1. Validate input exists
        if (!name || typeof name !== 'string') {
            return res.status(400).send('Invalid image name');
        }

        // 2. Sanitize filename to prevent path traversal and other attacks
        const sanitizedName = sanitizeFileName(name);
        if (!sanitizedName) {
            return res.status(400).send('Invalid characters in filename');
        }

        // 3. Construct safe file path using the sanitized name
        const imagePath = path.join(__dirname, 'images', sanitizedName);

        // 4. Check if the file exists and is a regular file (not a directory)
        if (!fs.existsSync(imagePath) || !fs.statSync(imagePath).isFile()) {
            return res.status(404).send('Image not found');
        }

        // 5. Set appropriate headers
        const mimeType = mime.lookup(imagePath);
        res.setHeader('Content-Type', mimeType);
        res.setHeader('Cache-Control', 'public, max-age=31536000'); // Cache for a year

        // 6. Stream the file to prevent memory issues with large files
        const stream = createReadStream(imagePath);
        
        stream.on('open', () => {
            stream.pipe(res);
        }).on('error', (err) => {
            res.status(500).send('Error reading image');
        });

    } catch (error) {
        console.error('Error serving image:', error);
        res.status(500).send('Internal server error');
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
