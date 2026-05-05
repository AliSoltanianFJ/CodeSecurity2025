const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ensure uploads directory exists
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

/**
 * save the uploaded pdf file to ./uploads directory
 */
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    // sanitize and give each file a unique name
    const name = file.originalname
      .replace(/\s+/g, '_')               // spaces → underscores
      .replace(/[^a-zA-Z0-9._-]/g, '');   // strip anything suspect
    const uniqueSuffix = Date.now();
    const ext = path.extname(name).toLowerCase();
    cb(null, `${uniqueSuffix}${ext}`);
  }
});

function fileFilter(req, file, cb) {
  // only accept actual PDFs
  if (file.mimetype !== 'application/pdf') {
    return cb(new Error('Only PDF files are allowed'), false);
  }
  cb(null, true);
}

const upload = multer({
  storage,
  fileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024   // max 5MB
  }
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  // multer has already saved the file at this point
  if (!req.file) {
    return res.status(400).json({ error: 'No file uploaded' });
  }
  res.status(200).json({
    message: 'File uploaded successfully',
    filename: req.file.filename,
    path: req.file.path
  });
});

// generic error handler to catch multer errors
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    // Multer-specific errors
    return res.status(400).json({ error: err.message });
  } else if (err) {
    // other errors
    return res.status(400).json({ error: err.message });
  }
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});