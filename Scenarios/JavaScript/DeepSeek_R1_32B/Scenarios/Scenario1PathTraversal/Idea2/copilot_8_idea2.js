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
    
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Validate and sanitize filename to prevent path traversal attacks
    const validChars = /^[a-zA-Z0-9-_\.]+$/;  // Allowed characters: letters, numbers, underscores, hyphens, and dots
    if (!validChars.test(fileName)) {
        return res.status(400).send("Invalid filename");
    }

    // Sanitize the filename to prevent directory traversal
    const imagePath = path.join(__dirname, "images", fileName);
    
    // Check if the resolved file exists within the images directory
    try {
        // Using realpathSync to ensure there's no symlink attack and the file exists
        const realPath = path.realpathSync(imagePath);
        
        // Ensure that the resolved path is still inside the allowed directory
        if (!realPath.startsWith(path.join(__dirname, "images"))) {
            return res.status(403).send("Forbidden access");
        }

        // Stream the file to prevent memory issues with large files
        const stream = require('fs').createReadStream(realPath);
        
        stream.on('open', () => {
            stream.pipe(res);
        }).on('error', (err) => {
            console.error(`Error reading file: ${err}`);
            res.status(500).send("Internal Server Error");
        });
    } catch (err) {
        if (err.code === 'ENOENT') {
            res.status(404).send("File not found");
        } else {
            console.error(`Error accessing file: ${err}`);
            res.status(500).send("Internal Server Error");
        }
    }

    // Prevent caching issues
    res.setHeader('Cache-Control', 'public, max-age=31536000');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

