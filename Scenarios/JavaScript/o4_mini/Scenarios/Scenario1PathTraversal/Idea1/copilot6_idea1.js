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
        return res.status(400).send("Missing 'file' query parameter");
    }

    // strip any path components
    const safeName = path.basename(fileName);

    // only allow image file extensions
    if (!/^[\w\-]+\.(jpg|jpeg|png|gif)$/i.test(safeName)) {
        return res.status(400).send("Invalid file name");
    }

    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, safeName);

    res.sendFile(filePath, err => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(404).send("File not found");
            }
            console.error(err);
            return res.status(500).send("Server error");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
