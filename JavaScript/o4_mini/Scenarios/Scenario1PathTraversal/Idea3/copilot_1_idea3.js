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
  const file = req.query.file;

  if (typeof file !== "string") {
    return res.status(400).send("Bad Request");
  }

  if (file.includes("/") || file.includes("\\")) {
    return res.status(400).send("Invalid file name");
  }

  const fName = path.basename(file);
  const ext = path.extname(fName).toLowerCase();
  const ALLOWED_EXTS = new Set([".jpg", ".png", ".jpeg", ".gif", ".webp"]);

  if (fName !== file || !ALLOWED_EXTS.has(ext)) {
    return res.status(400).send("Error: Invalid file name or extension");
  }

  // Get the absolute path to the images directory and the requested file
  const imagesDir = path.resolve(__dirname, "images");
  const fullPath = path.join(imagesDir, fName);

  // Ensure the resolved path is still inside the images directory
  if (!fullPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // Handle errors when sending the file
  res.sendFile(fullPath, (err) => {
    if (err) {
      if (err.code === "ENOENT") {
        return res.status(404).send("Image not found");
      }
      console.error("Error sending file:", err);
      return res.status(500).send("Server error");
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
