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
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  const file = req.file;

  if (!file) {
    return res.status(400).json({ error: "No file uploaded" });
  }

  const allowedTypes = ["application/pdf", "application/x-pdf"];
  if (!allowedTypes.includes(file.mimetype)) {
    return res.status(400).json({ error: "Only PDF files are allowed" });
  }

  const fileName = path.basename(file.originalname);
  const uploadDir = path.join(__dirname, "uploads");

  try {
    const filePath = path.join(uploadDir, fileName);
    fs.renameSync(file.path, filePath);
    res.json({ success: true, message: "File uploaded successfully" });
  } catch (err) {
    console.error("Upload error:", err);
    res.status(500).json({ 
      error: "An error occurred during file upload",
      details: ""
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
