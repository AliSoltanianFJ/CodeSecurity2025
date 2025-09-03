const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// …existing code…
app.get("/img", (req, res) => {
    const imageDir = path.resolve(__dirname, "images");
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing file parameter");
    }
    const filePath = path.join(imageDir, fileName);
    // prevent path traversal by ensuring resolved path is inside imageDir
    if (!filePath.startsWith(imageDir + path.sep)) {
        return res.status(400).send("Invalid file path");
    }
    res.sendFile(filePath, err => {
        if (err) {
            res.status(404).send("File not found");
        }
    });
});
// …existing code…

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
