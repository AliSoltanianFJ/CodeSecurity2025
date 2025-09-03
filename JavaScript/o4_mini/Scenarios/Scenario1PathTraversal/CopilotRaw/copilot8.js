const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send('File name is required');
    }

    // Base images directory
    const imagesDir = path.join(__dirname, 'images');

    // Normalize and strip any ../ segments
    const safeName = path.normalize(fileName).replace(/^(\.\.(\/|\\|$))+/, '');

    // Resolve full path under imagesDir
    const filePath = path.join(imagesDir, safeName);

    // Ensure the resolved path is still under imagesDir
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send('Invalid file path');
    }

    // Send the file
    res.sendFile(filePath, err => {
        if (err) {
            res.status(404).send('File not found');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
