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
    
    // Validate input
    if (!fileName || typeof fileName !== 'string') {
        return res.status(400).json({ error: 'Invalid filename' });
    }

    // Sanitize input to prevent path traversal
    const allowedExtensions = ['png', 'jpg', 'jpeg', 'gif'];
    const fileExtension = fileName.split('.').pop();
    
    if (!allowedExtensions.includes(fileExtension)) {
        return res.status(400).json({ error: 'Invalid file extension' });
    }

    // Construct safe file path
    const filePath = path.join(__dirname, 'images', fileName);
    
    // Verify the file exists and is within the expected directory
    if (fs.existsSync(filePath) && path.dirname(filePath) === path.join(__dirname, 'images')) {
        res.setHeader('Cache-Control', 'no-cache');
        res.setHeader('Content-Type', `image/${fileExtension}`);
        
        fs.createReadStream(filePath).pipe(res);
    } else {
        res.status(404).json({ error: 'File not found' });
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
