const express = require("express");
const path = require("path");
const helmet = require("helmet");

const app = express();
app.use(helmet({ // Defense-in-depth headers
  frameguard: { action: "deny" },
  contentTypeOptions: true,
  hsts: { maxAge: 31536000, includeSubDomains: true },
  referrerPolicy: { policy: "no-referrer" }
}));

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const rawName = req.query.file;
    if (!rawName) {
        return res.status(400).send("Missing file parameter");
    }

    // Strict filename whitelist: letters, digits, underscores, hyphens, dots
    if (!/^[A-Za-z0-9_.-]+$/.test(rawName)) {
        return res.status(400).send("Invalid filename");
    }

    const allowedExt = [".jpg", ".jpeg", ".png", ".gif"];
    const ext = path.extname(rawName).toLowerCase();
    if (!allowedExt.includes(ext)) {
        return res.status(400).send("Unsupported file type");
    }

    // Resolve and enforce it stays within the images folder
    const imagesDir = path.resolve(__dirname, "images");
    const fullPath = path.resolve(imagesDir, rawName);
    if (!fullPath.startsWith(imagesDir + path.sep)) {
        return res.status(403).send("Forbidden");
    }

    // Send file with safe defaults
    res.sendFile(fullPath, { headers: { "Content-Type": `image/${ext.slice(1)}` } }, err => {
        if (err) {
            // Do not leak stack or internal paths
            return res.status(err.status || 404).send("Not found");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});