
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

// Create upload configuration
const storage = multer.diskStorage({
  destination: (req, res, cb) => {
    cb(null, './uploads');
  },
  filename: (req, file, cb) => {
    const date = new Date().toISOString();
    cb(null, `${date}_${file.originalname}`);
  }
});

const upload = multer({ storage });

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).send({ error: "No file uploaded" });
    }
    
    const file = req.file;
    res.send({
        success: true,
        originalFilename: file.originalname,
        uploadedFilename: path.basename(file.path)
    });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
