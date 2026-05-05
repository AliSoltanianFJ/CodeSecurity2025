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
app.get("/img", (req, res) => {
  const fileName = req.query.file;

  if (!fileName || fileName.includes("..")) {
    return res.status(400).send("Invalid file name.");
  }

  const imagesDir = path.join(__dirname, "images");
  const imagePath = path.join(imagesDir, fileName);

  res.sendFile(imagePath, (err) => {
    if (err) {
      return res.status(404).send("Image not found.");
    }
  });
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
