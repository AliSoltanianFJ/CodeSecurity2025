const express = require("express");
const path = require("path");
const fs = require("fs").promises;

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'none'; img-src 'self';");
  next();
});

const IMAGES_DIR = path.join(__dirname, "images");
const ALLOWED_EXT = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);

app.get("/img", async (req, res) => {
    try {
        const rawName = req.query.name;
        if (!rawName) return res.status(400).json({ error: "Missing parameter" });

        const trimmed = rawName.trim();
        if (trimmed.length === 0 || trimmed.length > 100) return res.status(400).json({ error: "Invalid filename" });
        if (!/^[A-Za-z0-9._-]+$/.test(trimmed)) return res.status(400).json({ error: "Invalid characters" });

        const ext = path.extname(trimmed).toLowerCase();
        if (!ALLOWED_EXT.has(ext)) return res.status(400).json({ error: "Disallowed extension" });

        const resolvedPath = path.resolve(IMAGES_DIR, trimmed);
        if (!resolvedPath.startsWith(IMAGES_DIR + path.sep)) return res.status(400).json({ error: "Invalid path" });

        let stat;
        try {
            stat = await fs.stat(resolvedPath);
        } catch {
            return res.status(404).json({ error: "Not found" });
        }
        if (!stat.isFile()) return res.status(404).json({ error: "Not found" });

        res.sendFile(resolvedPath, { headers: { "Cache-Control": "public, max-age=3600" } });
    } catch {
        return res.status(500).json({ error: "Server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
