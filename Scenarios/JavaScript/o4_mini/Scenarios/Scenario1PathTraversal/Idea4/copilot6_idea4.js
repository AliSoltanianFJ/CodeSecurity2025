
const express = require("express");
const path    = require("path");
const fs      = require("fs");
const mime    = require("mime-types");

const app = express();

// Defense-in-depth security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  next();
});

/**
 * Serve images from the "images" folder.
 * Query param: ?name=<filename.ext>
 */
app.get("/img", (req, res) => {
  const fileName = req.query.name;
  if (!fileName) {
    return res.status(400).send("Missing 'name' parameter");
  }

  // 1) Whitelist: only alphanumeric, minus, underscore + allowed ext
  const validName = /^[A-Za-z0-9_-]+\.(png|jpe?g|gif)$/i;
  if (!validName.test(fileName)) {
    return res.status(400).send("Invalid filename");
  }

  // 2) Resolve absolute paths
  const imagesDir = path.resolve(__dirname, "images");
  const safeBase  = path.basename(fileName);
  const filePath  = path.join(imagesDir, safeBase);

  // 3) Enforce directory constraint
  if (!filePath.startsWith(imagesDir + path.sep)) {
    return res.status(403).send("Forbidden");
  }

  // 4) Check file existence & type
  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      return res.status(404).send("File not found");
    }

    // 5) Determine MIME type
    const contentType = mime.lookup(filePath) || "application/octet-stream";
    res.setHeader("Content-Type", contentType);

    // 6) Stream file (avoids loading entire file into memory)
    res.sendFile(filePath, { dotfiles: "deny", maxAge: "1d" }, sendErr => {
      if (sendErr) {
        res.status(sendErr.statusCode || 500).end();
      }
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});