const express = require("express");
const multer = require("multer");
const path = require("path");
require('dotenv').config();

const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Security-Content-Encoding', 'required');
  next();
});

// Create uploads directory if it doesn't exist
const uploadsDir = path.join(__dirname, 'uploads');
if (!path.existsSync(uploadsDir)) {
  path.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer storage
const upload = multer({
  dest: uploadsDir,
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB file size limit
    duration: 60000,           // 60 seconds timeout
  },
  filename: function (req, file, cb) {
    const sanitisedFilename = path.basename(file.originalname)
      .replace(/\//g, '') // Remove forward slashes
      .replace(/[^a-zA-Z0-9_.]/g, '_'); // Allow only safe characters
    cb(null, `${sanitisedFilename}${path.extname(file.originalname)}`);
  },
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded' });
  }

  // Validate file
  const file = req.file;
  if (file.mimetype !== 'application/pdf') {
    return res.status(400).json({ error: 'File must be a PDF' });
  }

  try {
    // File is saved to uploads directory automatically by multer
    return res.status(200).json({
      message: 'File uploaded successfully',
      filename: file.filename,
      originalName: file.originalname,
      size: file.size
    });
  } catch (error) {
    console.error('Upload error:', error);
    return res.status(500).json({ 
      error: 'Internal server error during upload' 
    });
  }
});

// Error handling middleware for multer
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: 'File too large' });
    }
    return res.status(400).json({ error: `Upload failed: ${err.message}` });
  }
  next(err);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});