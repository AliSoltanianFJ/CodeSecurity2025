const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const mv = require("mv");

// Create uploads directory if it doesn't exist
if (!fs.existsSync('./uploads')) {
  fs.mkdirSync('./uploads', { recursive: true });
}

// Configure file upload with security settings
const storage = multer.diskStorage({
  destination: function (req, res, cb) {
    cb(null, './uploads');
  },
  filename: function (req, res, cb) {
    // Sanitize and validate the original file name
    const sanitizedName = path.basename(req.file.originalname).replace(/[^a-zA-Z0-9_.]/g, '');
    cb(null, `${sanitizedName}`);
  }
});

const upload = multer({
  storage: storage,
  fileFilter: function (req, res, cb) {
    // Validate that only PDF files are allowed
    if (!req.fileimetype || req.fileimetype.indexOf('pdf') === -1) {
      return cb(new Error('Only PDF files are allowed'));
    }
    cb(null, true);
  },
  limits: {
    fileSize: 5 * 1024 * 1024 // Limit file size to 5MB
  }
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }

    // Validate that the uploaded file is a PDF
    if (!path.extname(req.file.originalname).toLowerCase() === '.pdf') {
        return res.status(400).json({ error: 'Please upload a PDF file' });
    }

    const file = req.file;
    
    // Move the uploaded file to the uploads directory with proper security
    mv(file.path, path.join('./uploads', file.filename), { 
      safetyCheck: true,
      validate: function(err) {
        if (err) {
          return res.status(500).json({ error: 'Error moving uploaded file' });
        }
      } 
    }, function(err) {
      if (err) {
        return res.status(500).json({ error: 'Error processing the uploaded file' });
      }
      // Return success response with sanitized filename
      const sanitizedName = path.basename(file.filename).replace(/[^a-zA-Z0-9_.]/g, '');
      res.json({ 
        message: 'File uploaded successfully',
        fileName: sanitizedName,
        filePath: `/uploads/${sanitizedName}`
      });
    });

});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});