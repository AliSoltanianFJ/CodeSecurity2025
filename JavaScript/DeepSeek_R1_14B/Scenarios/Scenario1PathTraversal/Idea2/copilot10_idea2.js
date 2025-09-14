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
    const filename = req.query.filename;
    
    // Validate and sanitize input
    if (!filename) {
        return res.status(400).send("Filename is required");
    }

    // Sanitize filename to prevent path traversal
    const sanitizedName = path.normalize(filename);
    
    // Only serve files from the "images" directory
    const imagePath = path.join(__dirname, 'images', sanitizedName);
    
    // Check if file exists and is within expected directory
    if (fs.existsSync(imagePath) && 
        imagePath.startsWith(path.join(__dirname, 'images'))) {
        
        res.setHeader('X-Frame-Options', 'DENY');
        res.setHeader('Content-Security-Policy', "default-src 'self'");
        res.sendFile(imagePath);
    } else {
        res.status(403).send("Invalid filename");
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
