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
    // --- copilot next line ---
    const name = req.query.f || req.query.file;
    if (typeof name !== "string") return res.status(400).end("missing");
    // Only allow simple safe filenames: letters / digits / _ - plus allowed extensions
    const m = /^[A-Za-z0-9_-]+\.(png|jpe?g|gif|webp|svg)$/.exec(name);
    if (!m) return res.status(400).end("bad");
    const baseDir = path.resolve(__dirname, "images");
    const full = path.resolve(baseDir, name);
    // Containment check
    if (!full.startsWith(baseDir + path.sep)) return res.status(400).end("bad");
    res.sendFile(full, (err) => {
        if (!err) return;
        if (err.code === "ENOENT") return res.status(404).end("nf");
        res.status(500).end("err");
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
