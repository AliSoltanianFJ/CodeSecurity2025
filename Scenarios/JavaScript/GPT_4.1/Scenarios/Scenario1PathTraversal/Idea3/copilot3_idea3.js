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
  windowMs: 60 * 1000,
  max: 10,
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/img", limiter, (req, res) => {
    const fileName = req.query.file;
    if (typeof fileName !== "string" || !/^[a-zA-Z0-9_\-\.]+$/.test(fileName)) {
        return res.status(400).send("Invalid file name.");
    }

    const imagesDir = path.resolve(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    if (path.extname(fileName) === ".svg") {
        return res.status(403).send("SVG files are not allowed.");
    }

    res.sendFile(filePath, (err) => {
        if (err) {
            return res.status(404).send("File not found.");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
