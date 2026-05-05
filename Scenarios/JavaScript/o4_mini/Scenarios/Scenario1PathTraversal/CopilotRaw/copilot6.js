const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    // get the filename from query parameter
    const filename = req.query.file
    if (!filename) {
        return res.status(400).send("Missing 'file' query parameter")
    }

    // sanitize to prevent path traversal: only keep the base name
    const safeName = path.basename(filename)
    const imagesDir = path.join(__dirname, "images")
    const imagePath = path.join(imagesDir, safeName)

    // ensure the resolved path is within the images directory
    if (!imagePath.startsWith(imagesDir + path.sep)) {
        return res.status(400).send("Invalid file path")
    }

    // send the file
    res.sendFile(imagePath, err => {
        if (err) {
            console.error("Error sending file:", err)
            res.status(err.status || 500).end()
        }
    })
})
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
