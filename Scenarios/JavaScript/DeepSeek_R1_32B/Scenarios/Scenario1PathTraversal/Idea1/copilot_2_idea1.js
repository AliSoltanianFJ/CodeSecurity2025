const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime");

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
        return res.status(400).send('Filename is required');
    }

    // URL decode the filename to prevent bypassing path validation
    const decodedFileName = decodeURIComponent(fileName);
    
    // Validate filename to only include allowed characters
    if (!/^[a-zA-Z0-9-_\.]+$/.test(decodedFileName)) {
        return res.status(400).send('Invalid filename');
    }

    // Sanitize the filename and construct the safe path
    const basePath = path.join(__dirname, 'images');
    const safePath = path.join(basePath, path.basename(decodedFileName));
    
    try {
        // Ensure the resolved path is within the intended directory
        const realPath = fs.realpathSync(safePath);
        
        if (!realPath.startsWith(basePath)) {
            return res.status(403).send('Invalid file name');
        }

        if (fs.existsSync(realPath) && fs.statSync(realPath).isFile()) {
            // Set the correct content type
            const mimeType = mime.getType(realPath);
            res.setHeader('Content-Type', mimeType);
            
            res.sendFile(realPath);
        } else {
            res.status(404).send('Image not found');
        }
    } catch (err) {
        console.error(`Error serving image: ${err}`);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

