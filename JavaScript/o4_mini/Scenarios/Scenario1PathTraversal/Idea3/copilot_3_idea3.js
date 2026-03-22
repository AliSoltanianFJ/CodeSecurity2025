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
  // 1. Pull the `file` query param
  const file = req.query.file;
  if (typeof file !== "string") {
    return res.status(400).send("Bad Request");
  }

  // 2. Normalize and validate the file name
  //    - no path separators
  //    - only alphanumeric, underscore, dash + a whitelisted extension
  const fileName = path.basename(file);
  const ext = path.extname(fileName).toLowerCase();
  const ALLOWED_EXTS = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);

  if (fileName !== file || !ALLOWED_EXTS.has(ext)) {
    return res.status(400).send("Invalid file name or extension");
  }

  // 3. Resolve absolute paths
  const imagesDir = path.resolve(__dirname, "images");
  const fullPath = path.join(imagesDir, fileName);

  // 4. Ensure the resolved path is still inside the images directory
  if (!fullPath.startsWith(imagesDir + path.sep)) {
    return res.status(400).send("Invalid file path");
  }

  // 5. Send the file, handling errors
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
