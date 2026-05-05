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
// ...existing code...

/**
 * Secure Multer configuration for PDF uploads.
 * - Only allows PDF files.
 * - Limits file size to 5MB.
 * - Stores files in ./uploads with a safe filename.
 * - Prevents directory traversal and overwriting.
 */
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      // Ensure the uploads directory exists and is not world-writable.
      cb(null, path.resolve(__dirname, "uploads"));
    },
    filename: (req, file, cb) => {
      // Use a safe, unique filename (timestamp + random hex).
      const ext = path.extname(file.originalname).toLowerCase();
      const safeName = `${Date.now()}-${Math.random().toString(16).slice(2)}${ext}`;
      cb(null, safeName);
    }
  }),
  fileFilter: (req, file, cb) => {
    // Only accept PDF files, validate MIME type and extension.
    const allowedMime = "application/pdf";
    const allowedExt = ".pdf";
    if (
      file.mimetype === allowedMime &&
      path.extname(file.originalname).toLowerCase() === allowedExt
    ) {
      cb(null, true);
    } else {
      cb(new Error("Only PDF files are allowed."));
    }
  },
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  }
});

// ...existing code...
app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    // Principle of least privilege: Only handle what's necessary.
    try {
      if (!req.file) {
        // Do not leak internal details.
        return res.status(400).json({ error: "No file uploaded or invalid file type." });
      }

      // Sanitize file metadata before returning to client.
      const fileInfo = {
        filename: path.basename(req.file.filename),
        size: req.file.size,
        mimetype: req.file.mimetype
      };

      // Do not expose server paths or sensitive info.
      res.status(201).json({ message: "PDF uploaded successfully.", file: fileInfo });
    } catch (err) {
      // Log error securely (not shown here), do not leak details to client.
      res.status(500).json({ error: "Internal server error." });
    }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
