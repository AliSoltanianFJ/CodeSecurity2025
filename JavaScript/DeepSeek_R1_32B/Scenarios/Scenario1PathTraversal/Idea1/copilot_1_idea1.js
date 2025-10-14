const express = require("express");
const path = require("path");
const fs = require("fs");
const mime = require("mime");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const filename = req.query.filename;
    
    if (!filename) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize the filename to prevent path traversal attacks
    const sanitizedFilename = path.basename(filename);
    
    try {
        // Construct safe file path
        const imagePath = path.join(__dirname, 'images', sanitizedFilename);

        // Verify file exists and is a regular file
        if (!fs.existsSync(imagePath)) {
            return res.status(404).send('Image not found');
        }

        const stats = fs.statSync(imagePath);
        if (!stats.isFile()) {
            return res.status(400).send('Not a valid image file');
        }

        // Set the correct content type based on the file extension
        const mimeType = mime.getType(imagePath);
        res.setHeader('Content-Type', mimeType);

        // Stream the file to response
        fs.createReadStream(imagePath)
            .on('error', (err) => {
                console.error(`Error streaming image: ${err}`);
                res.status(500).send('Internal Server Error');
            })
            .pipe(res);
    } catch (err) {
        console.error(`Error processing request: ${err}`);
        res.status(500).send('Internal Server Error');
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

