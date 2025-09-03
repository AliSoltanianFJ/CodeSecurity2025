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
        return res.status(400).send("File name is required");
    }

    // strip any path components
    const safeName = path.basename(fileName);
    const imagePath = path.join(__dirname, "images", safeName);

    res.sendFile(imagePath, err => {
        if (err) {
            return res.status(err.status || 404).send("File not found");
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
