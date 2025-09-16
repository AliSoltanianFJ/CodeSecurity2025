const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const sanitizeFilename = require("sanitize-filename");

// Set up middleware for file uploads
const upload = multer({
  storage: multer.diskStorage({
    destination: (req, res, cb) => {
      // Specify the directory to store uploaded files
      cb(null, "./uploads");
    },
    filename: (req, file, cb) => {
      // Sanitize and validate the filename
      const sanitizedName = sanitizeFilename(file.originalname);
      const extension = path.extname(file.originalname).toLowerCase();
      cb(null, `${sanitizedName}${extension}`);
    }
  }),
  limits: {
    fileSize: 5 * 1024 * 1024 // Limit file size to 5MB
  }
});

app.use(express.json()); // For parsing JSON request bodies

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
  // Validate that a file was uploaded
  if (!req.file) {
    return res.status(400).json({ error: "No file uploaded" });
  }

  try {
    // Check if the file is a PDF
    if (req.file.mimetype !== "application/pdf") {
      return res.status(400).json({ error: "Only PDF files are allowed" });
    }

    // Validate filename to prevent directory traversal and special characters
    const fileName = path.basename(req.file.originalname);
    if (!fileName || fileName.includes("..") || fileName.includes("/")) {
      return res.status(400).json({ error: "Invalid filename" });
    }

    // Save the file with sanitized name
    const filePath = path.join("./uploads", req.file.filename);

    // Handle any potential errors during file writing
    if (req.file.error) {
      throw req.file.error;
    }

    res.status(201).json({
      message: "File uploaded successfully",
      fileName: req.file.filename,
      filePath: `/uploads/${req.file.filename}`
    });

  } catch (error) {
    console.error("Upload error:", error);
    res.status(500).json({ error: "Internal server error during file upload" });
  }
});

app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});