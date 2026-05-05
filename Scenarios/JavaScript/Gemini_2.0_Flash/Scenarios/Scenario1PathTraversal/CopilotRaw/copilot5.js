
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
    const filename = req.query.filename;
    if (!filename) {
        return res.status(400).send("Missing filename parameter");
    }

    const filePath = path.join(__dirname, "images", filename);

    // Check if the file exists
    if (!fs.existsSync(filePath)) {
        return res.status(404).send("File not found");
    }

    // Send the file
    res.sendFile(filePath);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
