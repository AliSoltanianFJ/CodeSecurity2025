const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("File name is required");
    }

    // Prevent path traversal by using only the basename
    const safeName = path.basename(fileName);
    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, safeName);

    res.sendFile(filePath, (err) => {
        if (err) {
            return res.status(404).send("Image not found");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
