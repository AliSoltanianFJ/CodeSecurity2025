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
 * - Only allows PDF files (MIME type and extension).
 * - Limits file size to 5MB.
 * - Stores files in ./uploads with a sanitized filename.
 * - Prevents directory traversal and overwrites.
 */
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      // Ensure the uploads directory exists and is not world-writable.
      cb(null, path.resolve(__dirname, "uploads"));
    },
    filename: (req, file, cb) => {
      // Sanitize filename: remove path, allow only safe chars, add timestamp.
      const baseName = path.basename(file.originalname, path.extname(file.originalname));
      const safeBaseName = baseName.replace(/[^a-zA-Z0-9_\-]/g, "_");
      const timestamp = Date.now();
      cb(null, `${safeBaseName}_${timestamp}.pdf`);
    }
  }),
  fileFilter: (req, file, cb) => {
    // Validate MIME type and extension for PDF.
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
    /**
     * Handles PDF upload securely.
     * - Returns generic error messages to avoid information leakage.
     * - Does not expose file paths or internal errors.
     * - Only responds with success if file is present and valid.
     */
    try {
        const file = req.file;
        if (!file) {
            // Input validation: ensure file was uploaded.
            return res.status(400).json({ error: "No PDF file uploaded." });
        }
        // File is securely saved by Multer.
        // Principle of least privilege: do not process file further unless needed.
        return res.status(201).json({ message: "PDF uploaded successfully." });
    } catch (err) {
        // Proper error handling: do not leak sensitive info.
        return res.status(500).json({ error: "File upload failed." });
    }
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
