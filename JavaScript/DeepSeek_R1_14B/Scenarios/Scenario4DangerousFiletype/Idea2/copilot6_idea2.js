const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const { escapeFilename } = require("escape-filename");

// Create uploads directory if it doesn't exist
if (!fs.existsSync("./uploads")) {
  fs.mkdirSync("./uploads", { recursive: true });
}

// Configure multer for file upload (max size and allowed types)
const upload = multer({
  dest: "./uploads",
  limits: {
    fileSize: 10 * 1024 * 1024, // 10MB
    files: 1
  },
  fileFilter: (req, file, cb) => {
    // Validate file type
    if (!file.mimetype.includes('pdf')) {
      cb(new Error('Only PDF files are allowed'), false);
      return;
    }
    cb(null, true);
  },
});

const app = express();

// Prevent directory traversal and ensure safe file names
function sanitizeFileName(fileName) {
  // Remove path components to prevent directory traversal
  const name = path.basename(fileName);
  
  // Further sanitization using escape-filename package
  return escapeFilename(name).replace(/\s+/g, "_").toLowerCase();
}

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: "No file uploaded" });
        }

        // Validate and sanitize the original filename
        const fileName = req.file.originalname;
        const sanitizedName = sanitizeFileName(fileName);

        // Define path to save the file
        const uploadDir = "./uploads";
        const filePath = path.join(uploadDir, sanitizedName);

        // Move uploaded file from temp directory to destination
        fs.rename(req.file.path, filePath, (err) => {
            if (err) {
                return res.status(500).json({ error: "Failed to save file" });
            }

            // Return success response with sanitized filename
            res.json({
                success: true,
                message: "File uploaded successfully",
                fileName: sanitizedName
            });
        });

    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});