const express = require("express");
const mysql = require("mysql");
const multer = require("multer");
const path = require("path");
const helmet = require("helmet");

const app = express();

// Enable Helmet for additional security headers
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Configure multer with secure options
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        // Ensure upload directory is within the project root
        const uploadDir = path.join(__dirname, 'uploads');
        if (!path.isAbsolute(uploadDir)) {
            throw new Error('Upload directory must be absolute path');
        }
        fs.mkdirSync(uploadDir, { recursive: true });
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        // Use cryptographically secure hash for filename
        const filename = crypto.createHash('sha256')
            .update(Date.now().toString())
            .digest('hex') + '.pdf';
        cb(null, filename);
    }
});

const fileFilter = (req, file, cb) => {
    // Validate PDF files only
    if (!file.mimetype.startsWith('application/pdf')) {
        cb(new Error('Only PDF files are allowed'), false);
        return;
    }

    cb(null, true);
};

// Limit file size to prevent abuse
const maxSize = 10 * 1024 * 1024; // 10MB max file size

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: { fileSize: maxSize },
    onError: (err, next) => {
        console.error('Multer error:', err);
        next();
    }
});

// Sanitize input
function sanitizeFilename(originalname) {
    return originalname.replace(/[^a-zA-Z0-9.-]/g, '_');
}

app.post("/pdf/upload", upload.single("pdf"), (req, res) => {
    try {
        if (!req.file) {
            throw new Error('No file uploaded');
        }

        // Log basic information without exposing sensitive data
        const { originalname, size } = req.file;
        
        // Sanitize the filename for any use in logs or responses
        const sanitizedName = sanitizeFilename(originalname);
        console.log(`File uploaded: ${sanitizedName}, Size: ${size}`);

        res.status(200).json({
            status: 'success',
            message: 'PDF file uploaded successfully'
        });
    } catch (error) {
        console.error('Upload error:', error);
        res.status(400).json({
            status: 'error',
            message: 'Error uploading PDF file. Please check the requirements.'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

