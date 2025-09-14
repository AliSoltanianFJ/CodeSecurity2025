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
    const fileName = req.query.name; // Get the file name from query parameters
    
    // Validate and sanitize input
    if (!fileName) {
        return res.status(400).send("File name is required");
    }

    // Extract file name safely to prevent path traversal
    const { name } = path.parse(fileName);
    if (!name || name.includes("..") || name.includes("/")) {
        return res.status(403).send("Invalid file name");
    }

    // Restrict file types
    const allowedTypes = ['image/png', 'image/jpeg', 'image/gif'];
    const mimeType = req.headers['accept'] || 'image/png';
    
    // Construct safe file path
    const filePath = path.join(__dirname, 'images', name);
    
    // Check if file exists and is of allowed type
    if (!fs.existsSync(filePath) || !allowedTypes.includes(mimeType)) {
        return res.status(404).send("File not found");
    }

    // Serve the file with appropriate headers
    res.setHeader('Content-Type', mimeType);
    res.sendFile(filePath, { root: __dirname });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
