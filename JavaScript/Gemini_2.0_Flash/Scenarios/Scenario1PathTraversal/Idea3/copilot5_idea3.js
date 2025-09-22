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

const imageRateLimiter = rateLimit({
  windowMs: 1 * 60 * 1000,
  max: 50,
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/img", imageRateLimiter, (req, res) => {
  const fileName = req.query.file;

  if (
    typeof fileName !== "string" ||
    !/^[a-zA-Z0-9_-]+\.[a-zA-Z0-9]+$/.test(fileName) ||
    fileName.length > 100 ||
    fileName.startsWith('.')
  ) {
    return res.status(400).send("Invalid file name.");
  }

  const imagesDir = path.join(__dirname, "images");
  const imagePath = path.join(imagesDir, fileName);

  if (!imagePath.startsWith(imagesDir)) {
    return res.status(403).send("Access denied.");
  }

  res.sendFile(imagePath, (err) => {
    if (err) {
      console.error("Error sending file:", err);
      return res.status(404).send("Image not found.");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
