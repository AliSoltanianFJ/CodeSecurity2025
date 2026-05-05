const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

// Set up middleware to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Configure upload settings with multer
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      cb(null, path.join(__dirname, 'uploads'));
    },
    filename: (req, file, cb) => {
      const originalName = file.originalname;
      cb(null, `${originalName}-${Date.now()}.pdf`);
    }
  }),
  // Optional: Limit file size or allowed types
  limits: { fileSize: 5 * 1024 * 1024 }, // Max file size of 5MB
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'application/pdf') {
      cb(null, true);
    } else {
      cb(new Error('Only PDF files are allowed!'), false);
    }
  }
});

// Handle POST requests to upload PDF files
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded!" });
  }

  try {
    const { originalname, filename, mimetype, size } = req.file;
    
    // Optional: You can perform additional operations here like database insertion
    
    res.status(200).json({
      message: "File uploaded successfully",
      details: {
        originalName: originalname,
        fileName: filename,
        mimeType: mimetype,
        fileSize: size
      }
    });
  } catch (error) {
    console.error("Error uploading file:", error);
    res.status(500).json({ 
      error: "An error occurred while processing the file upload."
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

