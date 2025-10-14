const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const express = require("express");

const app = express();

// Configure upload settings with validation and security
const upload = multer({
  dest: './uploads/',
  limits: { fileSize: 500000 }, // Limit file size if needed
  fileFilter: (req, file, cb) => {
    // Only allow PDF files
    const allowedTypes = ['application/pdf'];
    if (!allowedTypes.includes(file.mimetype)) {
      return cb(new Error('Invalid file type. Only PDF is allowed.'), false);
    }
    cb(null, true);
  },
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.header("Content-Security-Policy", "default-src 'self'; script-src 'self' https:; style-src 'self' https:;");
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: "No file uploaded." });
    }

    const file = req.file;
    // Sanitize filename and ensure it is a PDF
    const sanitizedName = path.basename(file.filename, '.pdf') + '.pdf';
    try {
        // Move or process the file as needed (currently saved in uploads directory)
        res.status(200).json({
            message: "File uploaded successfully.",
            filename: sanitizedName,
            filePath: `/uploads/${sanitizedName}`
        });
    } catch (err) {
        console.error("Error processing upload:", err);
        return res.status(500).json({ error: "Internal server error." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});