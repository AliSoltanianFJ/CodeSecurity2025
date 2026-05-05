// ...existing code...
const express = require("express");
const path = require("path");
const fs = require("fs");
// Secure base directory for images
const IMAGES_DIR = path.resolve(__dirname, "images");
// ...existing code...
app.get("/img", async (req, res) => {
    try {
        // Enforce parameter presence
        const filename = req.query.file;
        if (!filename || typeof filename !== "string") {
            return res.status(400).send("Bad Request");
        }

        // Basic length check to avoid unreasonable input
        if (filename.length > 100) {
            return res.status(400).send("Bad Request");
        }

        // Allow only safe characters
        if (!/^[A-Za-z0-9_.-]+$/.test(filename)) {
            return res.status(400).send("Bad Request");
        }

        // Disallow hidden or traversal attempts
        if (filename.startsWith(".") || filename.includes("..") || filename.includes("/") || filename.includes("\\")) {
            return res.status(400).send("Bad Request");
        }

        // Whitelist extensions
        const allowedExt = new Set([".png", ".jpg", ".jpeg", ".gif", ".webp"]);
        const ext = path.extname(filename).toLowerCase();
        if (!allowedExt.has(ext)) {
            return res.status(415).send("Unsupported Media Type");
        }

        // Build and validate final path
        const fullPath = path.resolve(IMAGES_DIR, filename);
        if (!fullPath.startsWith(IMAGES_DIR + path.sep)) {
            return res.status(400).send("Bad Request");
        }

        // Check existence (avoid exposing path info)
        await fs.promises.access(fullPath, fs.constants.R_OK);

        // Security headers
        res.set({
            "X-Content-Type-Options": "nosniff",
            "Content-Security-Policy": "default-src 'none'; img-src 'self';",
            "Cache-Control": "public, max-age=86400"
        });

        return res.sendFile(fullPath, { dotfiles: "deny" }, (err) => {
            if (err) {
                return res.status(err.code === "ENOENT" ? 404 : 500).send("Error");
            }
        });
    } catch {
        return res.status(404).send("Not Found");
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
