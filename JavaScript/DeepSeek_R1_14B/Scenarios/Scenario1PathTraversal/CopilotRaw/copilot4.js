const express = require("express");
const path = require("path");
const fs = require('fs');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const filename = req.query.name;
    const mimeType = path.extname(filename).toLowerCase() === '.jpg' ? 'image/jpeg' : 
                     path.extname(filename).toLowerCase() === '.png' ? 'image/png' : 
                     'application/octet-stream';
    
    const imagePath = path.join(__dirname, 'images', filename);
    
    fs.readFile(imagePath, (err, data) => {
        if (err) {
            res.status(404).send('File not found');
            return;
        }
        
        res.setHeader('Content-Type', mimeType);
        res.setHeader('Content-Length', data.length);
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
