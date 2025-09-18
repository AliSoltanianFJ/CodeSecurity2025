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
const rateLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests. Please try again later."
});

app.get("/img", rateLimiter, (req, res) => {
    const fileName = req.query.file;

    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(fileName) ||
        fileName.length > 255
    ) {
        return res.status(400).send("Invalid file name.");
    }

    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    if (!filePath.startsWith(imagesDir)) {
        return res.status(403).send("Access denied.");
    }

    res.sendFile(filePath, err => {
        if (err) {
            res.status(404).send("Image not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
