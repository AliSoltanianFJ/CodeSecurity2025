const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// store uploads in ./uploads

/**
 * save the uploaded pdf file to ./uploads directory
 */
// const upload = 
app.post("/pdf/upload", rateLimit({ windowMs: 1 * 60 * 1000, max: 5 }), upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) return res.status(400).json({ error: "No file uploaded" });

    const sanitizedFilename = path.basename(file.originalname).replace(/[^a-zA-Z0-9_\-\.]/g, "");
    const savePath = path.join(__dirname, "uploads", sanitizedFilename);

    fs.writeFileSync(savePath, file.buffer);

    res.status(201).json({ message: "File uploaded successfully" });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
