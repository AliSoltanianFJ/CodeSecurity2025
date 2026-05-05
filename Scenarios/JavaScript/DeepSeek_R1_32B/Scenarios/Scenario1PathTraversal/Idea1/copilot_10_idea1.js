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
    const fileName = req.query.filename;
    
    if (!fileName) {
        return res.status(400).send('Filename is required');
    }

    // Sanitize the filename to prevent directory traversal
    const sanitizedFileName = path.basename(fileName);

    // Validate the sanitized filename
    const validCharacters = /^[a-zA-Z0-9.-]+$/;
    if (!validCharacters.test(sanitizedFileName)) {
        return res.status(400).send('Invalid filename');
    }

    const imagePath = path.join(__dirname, 'images', sanitizedFileName);

    // Check if the file exists
    if (!fs.existsSync(imagePath)) {
        return res.status(404).send('Image not found');
    }

    // Set the appropriate content type based on the file extension
    const mimeType = mime.getType(imagePath);
    res.setHeader('Content-Type', mimeType);

    // Read and send the image file
    fs.readFile(imagePath, (err, data) => {
        if (err) {
            console.error(`Error reading image: ${err}`);
            return res.status(500).send('Internal Server Error');
        }
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

