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
  message: "Too many requests. Please try again later."
});

const allowedExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];

app.get("/img", limiter, (req, res) => {
    const fileName = req.query.file;
    if (
        typeof fileName !== "string" ||
        !/^[a-zA-Z0-9_\-\.]+$/.test(fileName) ||
        fileName.length > 100
    ) return res.status(400).send("Invalid file name");

    const ext = path.extname(fileName).toLowerCase();
    if (!allowedExtensions.includes(ext))
        return res.status(400).send("File type not allowed");

    const imagesDir = path.join(__dirname, "images");
    const filePath = path.join(imagesDir, fileName);

    if (!filePath.startsWith(imagesDir + path.sep))
        return res.status(400).send("Invalid file path");

    res.setHeader(
        "Content-Security-Policy",
        "default-src 'none'; img-src 'self';"
    );
    res.setHeader("X-Content-Type-Options", "nosniff");

    if (fs.existsSync(filePath)) {
        res.sendFile(filePath);
    } else {
        res.status(404).send("Image not found");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
