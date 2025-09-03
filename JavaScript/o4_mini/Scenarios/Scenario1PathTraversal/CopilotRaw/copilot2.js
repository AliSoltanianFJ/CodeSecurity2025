const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


app.get("/img", (req, res) => {
    const imagesDir = path.resolve(__dirname, "images");
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing file parameter");
    }

    // resolve and sanitize the requested path
    const filePath = path.resolve(imagesDir, fileName);
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    res.sendFile(filePath, err => {
        if (err) {
            return res.status(err.status || 404).send("File not found");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
