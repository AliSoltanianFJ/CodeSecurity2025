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
    const fileName = req.query.filename;
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    const imagePath = path.join(__dirname, 'images', fileName);

    // Check if file exists
    if (!fs.existsSync(imagePath)) {
        return res.status(404).send("Image not found");
    }

    try {
        const imageBuffer = fs.readFileSync(imagePath);
        // Set appropriate content type based on the file extension
        const ext = path.extname(fileName).toLowerCase();
        let contentType = 'text/plain';
        
        switch(ext) {
            case '.png':
                contentType = 'image/png';
                break;
            case '.jpg':
            case '.jpeg':
                contentType = 'image/jpeg';
                break;
            case '.gif':
                contentType = 'image/gif';
                break;
            case '.webp':
                contentType = 'image/webp';
                break;
            default:
                // For unknown extensions, you might want to send a 400 error
                return res.status(400).send("Unsupported file type");
        }

        res.setHeader('Content-Type', contentType);
        res.send(imageBuffer);
    } catch (err) {
        console.error(err);
        res.status(500).send("Error serving image");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

