const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const nanoid = require("nanoid"); // For generating unique filenames

const app = express();

// Security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

// Configuration for multer file storage
const uploadDir = path.resolve(__dirname, './uploads');

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    // Ensure the uploads directory exists
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // Sanitize the filename and generate a unique ID
    const sanitizedName = file.originalname.replace(/[^a-zA-Z0-9.]/g, '_');
    const uniqueSuffix = nanoid();
    cb(null, `${sanitizedName}-${uniqueSuffix}.pdf`);
  }
});

// File validation middleware
const fileFilter = (req, file, cb) => {
  // Only allow PDF files
  if (file.mimetype !== 'application/pdf') {
    return cb(new Error('Invalid file type'), false);
  }

  const allowedExtensions = ['.pdf'];
  const extname = path.extname(file.originalname).toLowerCase();
  
  if (!allowedExtensions.includes(extname)) {
    return cb(new Error('Invalid extension'), false);
  }

  cb(null, true);
};

// Set up multer upload instance with security limits
const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: { // Limit file size to prevent abuse
    fileSize: 5 * 1024 * 1024 // 5MB max file size
  }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const { originalname, mimetype, path } = req.file;

        // Additional security checks
        if (mimetype !== 'application/pdf') {
            fs.unlinkSync(path); // Clean up the temporary file
            return res.status(400).json({ error: 'Invalid PDF file' });
        }

        // You might want to save this information in your database
        const fileInfo = {
            filename: originalname,
            filePath: path,
            uploadDate: new Date()
        };

        // Here you would typically insert into your database

        res.status(200).json({
            success: true,
            message: 'PDF file uploaded successfully',
            fileName: req.file.filename
        });
    } catch (error) {
        console.error('Error uploading PDF:', error);
        fs.unlinkSync(req.file.path); // Clean up the temporary file if upload fails
        res.status(500).json({ 
            success: false, 
            error: 'An error occurred while uploading the PDF' 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

