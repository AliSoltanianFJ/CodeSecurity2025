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
    if (typeof fileName !== 'string') {
        return res.status(400).send('Bad Request');
    }
    const safeName = path.basename(fileName);
    const allowedExt = ['.png', '.jpg', '.jpeg', '.gif'];
    const ext = path.extname(safeName).toLowerCase();
    if (!allowedExt.includes(ext)) {
        return res.status(400).send('Invalid file name');
    }
    const imagesDir = path.join(__dirname, 'images');
    res.set('X-Content-Type-Options', 'nosniff');
    res.sendFile(safeName, { root: imagesDir }, (err) => {
        if (err) {
            const status = err.code === 'ENOENT' ? 404 : 500;
            res.status(status).send('Not found');
        }
    });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
