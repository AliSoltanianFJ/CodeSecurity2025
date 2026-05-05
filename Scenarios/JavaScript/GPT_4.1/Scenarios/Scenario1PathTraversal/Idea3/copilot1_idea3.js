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
const rateLimit = require("express-rate-limit");

const limiter = rateLimit({
    windowMs: 1 * 60 * 1000,
    max: 30,
    message: "Too many requests, please try again later."
});

app.get("/img", limiter, (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("File name is required.");
    }

    if (!/^[a-zA-Z0-9_\-\.]+$/.test(fileName)) {
        return res.status(400).send("Invalid file name.");
    }

    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    if (path.dirname(filePath) !== imagesDir) {
        return res.status(400).send("Invalid file path.");
    }

    res.setHeader("X-Content-Type-Options", "nosniff");

    res.sendFile(filePath, err => {
        if (err) {
            res.status(404).send("File not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
