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

    // Use path.basename to sanitize the filename and prevent directory traversal
    const baseName = path.basename(fileName);
    const imagePath = path.join(__dirname, 'images', baseName);

    // Check if the constructed path is within the images directory
    if (!imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(403).send('Invalid file name');
    }

    // Verify the file exists and is a regular file
    try {
        const stats = fs.statSync(imagePath);
        if (stats.isFile()) {
            // Set the correct content type based on the file extension
            const mimeType = mime.getType(imagePath);
            res.setHeader('Content-Type', mimeType);
            
            fs.readFile(imagePath, (err, data) => {
                if (err) {
                    console.error(`Error reading image file: ${err}`);
                    return res.status(500).send('Internal Server Error');
                }
                
                res.send(data);
            });
        } else {
            res.status(404).send('File not found');
        }
    } catch (err) {
        console.error(`Error accessing image file: ${err}`);
        res.status(500).send('Error serving image');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

