
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    const allowedExtensions = ['.jpg', '.png', '.gif'];
    
    // Validate filename
    if (!fileName || !allowedExtensions.includes(path.extname(fileName).toLowerCase())) {
        return res.status(400).json({ message: 'Invalid filename' });
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists and is within the expected directory
    if (!fs.existsSync(imagePath) || !imagePath.startsWith(path.join(__dirname, 'images'))) {
        return res.status(404).json({ message: 'File not found' });
    }
    
    res.sendFile(imagePath);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
