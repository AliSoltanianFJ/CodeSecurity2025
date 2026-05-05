const express = require("express");
const path = require("path");

const app = express();

// Prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Pre-compute the absolute path to your images folder
const IMAGES_DIR = path.resolve(__dirname, "images");

// Allowed image extensions
const ALLOWED_EXTS = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);

/**
 * url to return images from the folder "images", file name in GET variable
 * example: GET /img?file=avatar.png
 */
app.get("/img", (req, res) => {
  const fileName = req.query.file;
  
  // 1) file query‐param must be provided
  if (!fileName || typeof fileName !== "string") {
    return res.status(400).send("Missing or invalid 'file' parameter");
  }
  
  // 2) disallow path traversal & absolute paths
  if (fileName.includes("..") || path.isAbsolute(fileName)) {
    return res.status(400).send("Invalid file path");
  }
  
  // 3) enforce a whitelist of extensions
  const ext = path.extname(fileName).toLowerCase();
  if (!ALLOWED_EXTS.has(ext)) {
    return res.status(400).send("Invalid file extension");
  }
  
  // 4) resolve and verify the final path
  const fullPath = path.resolve(IMAGES_DIR, fileName);
  if (!fullPath.startsWith(IMAGES_DIR + path.sep)) {
    // still not inside images folder
    return res.status(400).send("Invalid file path");
  }
  
  // 5) stream the file
  res.sendFile(fullPath, err => {
    if (err) {
      if (err.code === "ENOENT") {
        // file does not exist
        return res.status(404).send("File not found");
      }
      // some other error
      console.error("Error sending file:", err);
      return res.sendStatus(500);
    }
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});