const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const sanitizeFilename = require('express-sanitize').sanitizeFilename;

const app = express();

// Add Content Security Policy headers to prevent clickjacking and XSS
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' https: 'unsafe-inline'; style-src 'self' https: 'unsafe-inline'");
  next();
});

// Configure multer for secure file uploads
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = path.join(__dirname, 'uploads');
    // Ensure directory exists
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Sanitize the filename and append extension
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const sanitizedName = sanitizeFilename(file.originalname.split('.')[0]);
    cb(null, `${sanitizedName}-${uniqueSuffix}.pdf`);
  }
});

// Validate PDF file type
const fileFilter = (req, file, cb) => {
  if (!file.mimetype.startsWith('application/pdf')) {
    return cb(new Error('Only PDF files are allowed'), false);
  }
  cb(null, true);
};

// Set up upload limits to prevent abuse
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: { fileSize: 1024 * 1024 * 5 } // 5MB maximum file size
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        const file = req.file;
        
        if (!file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // Verify that the file is a PDF
        if (file.mimetype !== 'application/pdf') {
            fs.unlinkSync(file.path); // Clean up invalid files
            return res.status(400).json({ error: "Only PDF files are allowed" });
        }

        // Log the upload details without exposing sensitive paths
        console.log(`File uploaded successfully.`);

        return res.status(200).json({
            filename: file.filename,
            originalname: sanitizeFilename(file.originalname),
            size: file.size,
            mimetype: file.mimetype
        });

    } catch (error) {
        console.error("Upload failed:", error);
        if (error.code === 'LIMIT_FILE_SIZE') {
            res.status(413).json({ error: "File too large. Maximum allowed size is 5MB." });
        } else {
            res.status(500).json({ error: "Internal server error while processing file upload" });
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});