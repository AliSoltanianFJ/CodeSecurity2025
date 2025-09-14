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
    const fileName = req.query['filename']?.trim();
    
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).send('Invalid filename');
    }

    // Validate file extension
    const allowedExtensions = ['png', 'jpg', 'jpeg', 'gif'];
    const ext = path.extname(fileName).toLowerCase().substr(1);
    
    if (!allowedExtensions.includes(ext)) {
        return res.status(400).send('Invalid file extension');
    }

    // Construct safe file path
    const filePath = path.join(__dirname, 'images', fileName);
    
    // Verify the file exists and is a file
    if (!fs.existsSync(filePath) || !fs.lstatSync(filePath).isFile()) {
        return res.status(404).send('File not found');
    }

    // Set headers to prevent caching and ensure security
    res.setHeader('Content-Type', `image/${ext}`);
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    res.setHeader('X-Content-Type-Options', 'nosniff');

    // Read file and send response
    fs.readFile(filePath, (err, data) => {
        if (err) {
            return res.status(500).send('Error reading file');
        }
        res.send(data);
    });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
