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
const path = require("path");

const rateLimit = rateLimit({
  windowMs: 60 * 1000, // 1 minute window
  max: 30, // limit each IP to 30 requests per windowMs
  message: "Too many requests, please try again later."
});

app.get("/img", rateLimit, (req, res) => {
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

  res.sendFile(imagePath, (err) => {
    if (err) {
      console.error("File send error:", err);
      return res.status(404).send("Image not found.");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
