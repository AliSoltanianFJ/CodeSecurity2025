const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const { sanitizeFilename } = require("sanitize-filename");

// Configure multer storage for PDF uploads
const uploadDir = path.join(__dirname, "uploads");
const mkdirSync = require('mkdirp').sync;

// Create uploads directory if it doesn't exist
if (!path.existsSync(uploadDir)) {
    mkdirSync(uploadDir, { recursive: true });
}

const fileStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + Math.round(Math.random() * 1E9).toString(36);
    const originalName = sanitizeFilename(file.originalname);
    const extension = path.extname(originalName).toLowerCase();
    
    // Only allow PDF files
    if (extension !== ".pdf") {
      return cb(new Error("Invalid file type"));
    }

    cb(null, `${uniqueSuffix}${extension}`);
  },
});

const upload = multer({
  storage: fileStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // Maximum file size of 5MB
  fileFilter(req, file, cb) {
    const allowedTypes = ["application/pdf"];
    if (!allowedTypes.includes(file.mimetype)) {
      return cb(new Error("Only PDF files are allowed"));
    }
    cb(null, true);
  },
});

const app = express();

// Add security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https:; style-src 'self' https:");
  // Add more security headers as needed

  // Sanitize all input
  req.sanitizeBody(['file.*']);
  
  next();
});

app.use(express.json({
  limit: '5mb',
  reviver: (key, value) => {
    if (typeof value === 'string') {
      return sanitizeFilename(value);
    }
    return value;
  },
}));

// Secure PDF upload endpoint
app.post("/pdf/upload", upload.single("pdf"), async (req, res) => {
  try {
    // Check for errors
    if (!req.file || !req.file.originalname) {
      throw new Error("No file uploaded");
    }

    const file = req.file;

    // Validate file properties
    if (file.size === 0) {
      throw new Error("Empty file uploaded");
    }

    // Ensure filename is sanitized
    const safeFilename = sanitizeFilename(file.originalname);

    // Log file details for auditing
    console.log({
      filename: safeFilename,
      size: file.size,
      type: file.mimetype,
      path: file.path
    });

    res.status(200).json({
      message: "PDF uploaded successfully",
      filename: safeFilename,
      size: file.size,
      path: file.path
    });
  } catch (error) {
    console.error("Upload error:", error);
    res.status(400).json({
      error: error.message,
      details: "Invalid file or upload attempt"
    });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

