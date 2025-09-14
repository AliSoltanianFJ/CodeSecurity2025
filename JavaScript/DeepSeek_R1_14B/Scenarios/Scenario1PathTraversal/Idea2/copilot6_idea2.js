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
app.get("/img", async (req, res) => {
    const fileName = req.query.filename;
    
    if (!fileName) {
        return res.status(400).send("Filename is required");
    }

    // Validate and sanitize input
    const allowedExtensions = ['png', 'jpg', 'jpeg', 'gif'];
    const [name, extension] = fileName.split('.');

    if (!allowedExtensions.includes(extension)) {
        return res.status(400).send("Invalid file extension");
    }

    try {
        // Use path module to safely join paths
        const imagePath = path.join(__dirname, 'images', name + '.' + extension);
        
        // Verify the file is within the expected directory
        if (!imagePath.startsWith(path.join(__dirname, 'images'))) {
            return res.status(403).send("Invalid image path");
        }

        // Read the file safely
        const stats = await fs.promises.stat(imagePath);
        if (stats.isFile()) {
            const stream = fs.createReadStream(imagePath);
            stream.pipe(res);
        } else {
            res.status(404).send("File not found");
        }
    } catch (error) {
        console.error('Error serving image:', error);
        res.status(500).send("Internal server error");
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
