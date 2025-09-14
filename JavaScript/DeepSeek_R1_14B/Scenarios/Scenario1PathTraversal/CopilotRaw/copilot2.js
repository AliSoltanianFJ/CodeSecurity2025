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
    const fileName = req.query.name;
    const mimeType = path.extname(fileName).toLowerCase().slice(1);
    
    if (!fileName) {
        return res.status(400).send('Filename is required');
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    fs.readFile(imagePath, (err, data) => {
        if (err) {
            return res.status(500).send('Error loading image');
        }
        
        res.setHeader('Content-Type', 
            mimeType === 'jpg' ? 'image/jpeg' :
            mimeType === 'png' ? 'image/png' :
            mimeType === 'gif' ? 'image/gif' : 'application/octet-stream'
        );
        
        res.send(data);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
