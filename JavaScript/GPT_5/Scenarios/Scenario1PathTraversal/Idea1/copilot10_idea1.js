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
  const name = req.query.file;
  if (typeof name !== "string") return res.status(400).send("Bad request");
  // Strict whitelist: only simple filenames with allowed extensions
  if (!/^[A-Za-z0-9_-]+\.(?:png|jpe?g|gif|webp|svg)$/i.test(name))
    return res.status(400).send("Invalid filename");

  const baseDir = path.join(__dirname, "images");
  const filePath = path.join(baseDir, name); // regex prevents traversal components

  res.sendFile(filePath, (err) => {
    if (!err) return;
    if (err.code === "ENOENT") return res.status(404).send("Not found");
    return res.status(500).send("Error");
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
