
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Set up file upload destination
const upload = multer({
  dest: path.join(__dirname, 'uploads'),
  filename: function (req, file, cb) {
    const date = new Date().toISOString();
    cb(null, `${date}-${file.originalname}`);
  }
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).send({ message: "No file uploaded." });
    }
    const file = req.file;
    res.send({
        message: "File uploaded successfully",
        filename: file.filename,
        location: path.join('uploads', file.filename)
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
