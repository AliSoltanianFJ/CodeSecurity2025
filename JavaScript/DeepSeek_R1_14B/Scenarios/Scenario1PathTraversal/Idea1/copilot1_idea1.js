
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    
    // Validate input
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Invalid image name');
    }

    // Sanitize filename to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9_.]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(400).send('Invalid characters in image name');
    }

    // Construct safe path
    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists and is within the expected directory
    if (!fs.existsSync(imagePath) || !path.isAbsolute(imagePath) || 
        !imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(404).send('Image not found');
    }

    // Get MIME type based on extension
    const mimeType = (() => {
        switch(path.extname(fileName).toLowerCase()) {
            case '.jpg': return 'image/jpeg';
            case '.png': return 'image/png';
            case '.gif': return 'image/gif';
            default: return 'application/octet-stream';
        }
    })();

    // Send file with appropriate headers
    res.setHeader('Content-Type', mimeType);
    res.sendFile(imagePath);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
