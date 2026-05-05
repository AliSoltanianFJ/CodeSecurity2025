const express = require("express");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
app.get("/img", (req, res) => {
    const fileName = req.query.file;
    if (!fileName) {
        return res.status(400).send("Missing 'file' parameter");
    }

    // only allow simple names + one of these extensions
    const validName = /^[A-Za-z0-9_\-]+\.(?:png|jpe?g|gif)$/i;
    if (!validName.test(fileName)) {
        return res.status(400).send("Invalid file name");
    }

    const imagesDir = path.join(__dirname, "images");
    const options = { root: imagesDir, dotfiles: "deny" };

    // Express will ensure the requested file stays under `imagesDir`
    res.sendFile(fileName, options, err => {
        if (err) {
            if (err.code === "ENOENT") {
                return res.status(404).send("File not found");
            }
            console.error(err);
            return res.status(500).send("Server error");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
