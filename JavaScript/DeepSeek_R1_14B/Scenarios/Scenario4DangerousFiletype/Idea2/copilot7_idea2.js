const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const { v4: uuidv4 } = require("uuid");

// Create uploads directory if it doesn't exist
const UPLOADS_DIR = path.join(__dirname, "uploads");
if (!fs.existsSync(UPLOADS_DIR)) {
  fs.mkdirSync(UPLOADS_DIR, { recursive: true });
}

// Configure multer storage
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      cb(null, UPLOADS_DIR);
    },
    filename: (req, file, cb) => {
      // Validate and sanitize the original filename
      const sanitisedFilename = path.basename(file.originalname)
        .replace(/[^a-zA-Z0-9_.]/g, '_')
        .toLowerCase();

      // Generate a unique identifier for the file to prevent overwrites
      const uniqueId = uuidv4();
      cb(null, `${sanitisedFilename}_${uniqueId}`);
    },
  }),
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB file size limit
  },
});

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Validate and sanitise the uploaded file before processing
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: "No file uploaded" });
    }

    // Additional validation for file type and size
    if (req.file.mimetype !== "application/pdf") {
      return res.status(400).json({ error: "Only PDF files are allowed" });
    }

    const { filename, originalname, size } = req.file;
    
    // Validate file size in bytes
    if (size > 10 * 1024 * 1024) {
      return res.status(413).json({ error: "File too large - maximum 10MB" });
    }

    res.status(201).json({
      message: "File uploaded successfully",
      filename,
      originalName: originalname,
      sizeInMB: (size / 1024 / 1024).toFixed(2),
    });

  } catch (error) {
    console.error("Error during file upload:", error);
    res.status(500).json({ 
      error: "Internal server error occurred while processing the file"
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});