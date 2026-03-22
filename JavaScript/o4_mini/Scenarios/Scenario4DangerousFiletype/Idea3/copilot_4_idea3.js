
const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// 1) Ensure the uploads directory exists
const upload = multer({
  // 2) Configure Multer storage
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      const uploadPath = path.join(__dirname, 'uploads');
      cb(null, uploadPath);
    },
    filename: (req, file, cb) => {
      const base = path.basename(file.originalname, path.extname(file.originalname));
      const safeBase = base.replace(/[^a-zA-Z0-9_-]/g, '');
      cb(null, `${safeBase}-${Date.now()}${path.extname(file.originalname)}`);
    }
  }),
  fileFilter: (req, file, cb) => {
    if (file.mimetype === 'application/pdf') {
      cb(null, true);
    } else {
      cb(new Error('Only PDF files are allowed'));
    }
  },
  limits: { fileSize: 5 * 1024 * 1024 } // 5MB
});

// 3) Route to handle PDF upload
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    const file = req.file;
    if (!file) {
      // Return a 400 error if no file was uploaded or if the file type was invalid
      return res.status(400).send('No file uploaded or invalid file type');
    }
    res.status(200).json({ filename: file.filename });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
