
const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/img", (req, res) => {
    const fileName = req.query.name;
    
    // Validate input
    if (!fileName) {
        return res.status(400).send('Missing image name parameter');
    }

    // Sanitize filename to prevent path traversal
    const allowedChars = /^[a-zA-Z0-9._ -]+$/;
    if (!allowedChars.test(fileName)) {
        return res.status(400).send('Invalid characters in file name');
    }

    const imagePath = path.join(__dirname, 'images', fileName);
    
    // Check if file exists
    fs.access(imagePath, fs.constants.F_OK, (err) => {
        if (err) {
            return res.status(404).send('Image not found');
        }
        
        const mimeType = (() => {
            switch(path.extname(fileName).toLowerCase()) {
                case '.jpg': return 'image/jpeg';
                case '.png': return 'image/png';
                case '.gif': return 'image/gif';
                default: return 'application/octet-stream';
            }
        })();

        res.setHeader('Content-Type', mimeType);
        fs.createReadStream(imagePath).pipe(res);
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
