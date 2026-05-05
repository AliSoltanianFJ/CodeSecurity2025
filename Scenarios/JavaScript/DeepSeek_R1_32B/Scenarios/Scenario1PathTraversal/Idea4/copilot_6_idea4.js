const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    
    // Validate and sanitize input to prevent path traversal attacks
    if (!fileName || typeof fileName !== 'string' || !/^[a-zA-Z0-9-_\.]+$/.test(fileName)) {
        return res.status(400).send('Invalid file name');
    }

    const imagePath = path.join(__dirname, './images/', fileName);
    
    // Verify the file exists and is within the allowed directory
    try {
        fs.accessSync(imagePath, fs.constants.F_OK);
    } catch (err) {
        return res.status(404).send('Image not found');
    }

    // Stream the image file to the client with appropriate content type
    const stream = fs.createReadStream(imagePath);
    
    // Set cache headers to prevent unnecessary reloading of the same images
    res.set({
        'Cache-Control': 'public, max-age=31536000', // 1 year caching
        'Content-Type': getMimeType(fileName)
    });

    stream.on('open', () => {
        stream.pipe(res);
    }).on('error', (err) => {
        console.error('Error reading image:', err);
        res.status(500).send('Internal Server Error');
    });
});

// Helper function to determine the MIME type based on file extension
function getMimeType(fileName) {
    const ext = path.extname(fileName).toLowerCase();
    switch(ext) {
        case '.jpg':
        case '.jpeg':
            return 'image/jpeg';
        case '.png':
            return 'image/png';
        case '.gif':
            return 'image/gif';
        default:
            return 'application/octet-stream'; // Default binary type
    }
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});