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
 * - Stores files in ./uploads with sanitized filenames.
 * - Prevents directory traversal and overwrites.
 */
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      // Ensure the uploads directory exists and is not world-writable.
      cb(null, path.resolve(__dirname, "uploads"));
    },
    filename: (req, file, cb) => {
      // Sanitize filename: only allow alphanumeric, dash, underscore, and .pdf extension.
      const baseName = path.basename(file.originalname, path.extname(file.originalname));
      const safeBaseName = baseName.replace(/[^a-zA-Z0-9-_]/g, "");
      const timestamp = Date.now();
      cb(null, `${safeBaseName}-${timestamp}.pdf`);
    }
  }),
  fileFilter: (req, file, cb) => {
    // Only accept PDF files by MIME type and extension.
    if (
      file.mimetype === "application/pdf" &&
      path.extname(file.originalname).toLowerCase() === ".pdf"
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
    try {
        // Validate that a file was uploaded
        if (!req.file) {
            // Do not leak internal error details
            return res.status(400).json({ error: "No PDF file uploaded." });
        }

        // Additional security: check file path is within uploads directory
        const uploadsDir = path.resolve(__dirname, "uploads");
        const uploadedPath = path.resolve(req.file.path);
        if (!uploadedPath.startsWith(uploadsDir)) {
            // Potential directory traversal detected
            return res.status(400).json({ error: "Invalid file path." });
        }

        // Respond with success, do not expose sensitive file system info
        res.status(201).json({ message: "PDF uploaded successfully." });
    } catch (err) {
        // Log error securely (not shown here), do not leak details to client
        res.status(500).json({ error: "Internal server error." });
    }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
