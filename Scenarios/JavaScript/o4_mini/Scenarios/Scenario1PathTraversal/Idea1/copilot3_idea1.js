const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const imagesDir = path.resolve(__dirname, "images");
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing 'file' query parameter");
    }

    // sanitize: keep only the base name (no folders)
    const safeName = path.basename(fileName);
    const filePath = path.join(imagesDir, safeName);

    // ensure the resolved path is still within imagesDir
    if (!filePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }

    res.sendFile(filePath, err => {
        if (err) {
            console.error("Error sending file:", err);
            if (err.code === "ENOENT") {
                return res.status(404).send("File not found");
            }
            return res.status(500).send("Server error");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
