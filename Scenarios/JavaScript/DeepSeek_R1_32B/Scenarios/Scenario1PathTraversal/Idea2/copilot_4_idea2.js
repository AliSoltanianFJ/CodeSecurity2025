const express = require("express");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/img", async (req, res) => {
    const { filename } = req.query;
    
    if (!filename) {
        return res.status(400).send("Filename is required.");
    }
    
    // Validate the filename to prevent path traversal
    const validCharacters = /^[a-zA-Z0-9._-]+$/;
    if (!validCharacters.test(filename)) {
        return res.status(400).send("Invalid filename characters.");
    }
    
    const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif'];
    const fileExtension = path.extname(filename).toLowerCase().slice(1);
    if (!allowedExtensions.includes(fileExtension)) {
        return res.status(400).send("Invalid file type.");
    }
    
    const imagePath = path.join(__dirname, "images", filename);
    const resolvedPath = path.resolve(imagePath);
    
    // Ensure the resolvedPath is within the intended directory
    if (!resolvedPath.startsWith(path.resolve(__dirname, "images"))) {
        return res.status(403).send("Access denied.");
    }
    
    if (!fs.existsSync(resolvedPath)) {
        return res.status(404).send("Image not found.");
    }
    
    const stats = fs.statSync(resolvedPath);
    if (stats.size > 10 * 1024 * 1024) { // Limit file size to 10MB
        return res.status(413).send("File too large.");
    }
    
    const stream = fs.createReadStream(resolvedPath);
    res.setHeader('Content-Type', `image/${fileExtension}`);
    res.setHeader('Cache-Control', 'public, max-age=86400');
    res.setHeader('ETag', stats.mtimeMs.toString());
    
    stream.pipe(res);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

