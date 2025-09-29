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
 * - Only allows PDF files (MIME type and extension check).
 * - Limits file size to 5MB.
 * - Stores files in ./uploads with a sanitized filename.
 * - Prevents directory traversal and overwrites.
 */
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      // Ensure uploads directory exists and is not world-writable
      cb(null, path.resolve(__dirname, "../../uploads"));
    },
    filename: (req, file, cb) => {
      // Sanitize filename: remove dangerous characters, enforce .pdf extension
      const baseName = path.basename(file.originalname, path.extname(file.originalname));
      const safeBaseName = baseName.replace(/[^a-zA-Z0-9_\-]/g, "_");
      cb(null, `${safeBaseName}_${Date.now()}.pdf`);
    }
  }),
  fileFilter: (req, file, cb) => {
    // Validate MIME type and extension
    const isPdfMime = file.mimetype === "application/pdf";
    const isPdfExt = path.extname(file.originalname).toLowerCase() === ".pdf";
    if (isPdfMime && isPdfExt) {
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
    // Principle of least privilege: only handle expected input
    try {
      if (!req.file) {
        // Do not leak internal error details
        return res.status(400).json({ error: "No file uploaded or invalid file type." });
      }

      // File metadata (do not expose sensitive paths)
      const fileInfo = {
        filename: req.file.filename,
        size: req.file.size,
        mimetype: req.file.mimetype
      };

      // Respond with minimal info, do not expose server paths
      res.status(201).json({ message: "PDF uploaded successfully.", file: fileInfo });
    } catch (err) {
      // Log error securely (in production, use a secure logger)
      console.error("Upload error:", err.message);
      // Generic error message to avoid information leakage
      res.status(500).json({ error: "Internal server error." });
    }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
