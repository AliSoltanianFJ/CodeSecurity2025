const express = require("express");
const path = require("path");
const crypto = require("crypto"); // For generating secure filenames
const fs = require("fs"); // For checking file existence

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Security Headers
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'self'"); //Strictest CSP
  res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  next();
});

// Rate Limiting (to prevent DoS) - install with npm install express-rate-limit
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: "Too many requests from this IP, please try again after 15 minutes"
});
app.use(limiter);

// Input Validation & Path Sanitization
app.get("/img", (req, res) => {
    const filename = req.query.name;

    if (!filename) {
        return res.status(400).send("Missing filename parameter");
    }

    // Validate filename (strict allow list - only alphanumeric and specific extensions)
    if (!/^[a-zA-Z0-9\.]+$/.test(filename)) {
        return res.status(400).send("Invalid filename: only alphanumeric characters and '.' are allowed");
    }

    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif'];
    const fileExtension = path.extname(filename).toLowerCase();
    if (!allowedExtensions.includes(fileExtension)) {
        return res.status(400).send("Invalid file extension. Allowed extensions are: " + allowedExtensions.join(", "));
    }

    // Create a secure, randomized filename to prevent path traversal
    const hashedFilename = crypto.createHash('sha256').update(filename + Date.now()).digest('hex') + fileExtension;
    const imagesDirectory = path.join(__dirname, 'images'); // Absolute path to images directory
    const filePath = path.join(imagesDirectory, hashedFilename); // Construct the full file path

    // Check if the file exists in the images directory
    fs.access(filePath, fs.constants.F_OK, (err) => {
        if (err) {
            console.error("File not found:", filename, filePath); // Log the attempted file access (without user input)
            return res.status(404).send("File not found");
        }

        // Set appropriate Content-Type header based on file extension
        let contentType = 'application/octet-stream'; // Default
        if (fileExtension === '.jpg' || fileExtension === '.jpeg') {
            contentType = 'image/jpeg';
        } else if (fileExtension === '.png') {
            contentType = 'image/png';
        } else if (fileExtension === '.gif') {
            contentType = 'image/gif';
        }

        res.setHeader('Content-Type', contentType);

        // Stream the file to the response (safer than sending the whole file at once)
        const fileStream = fs.createReadStream(filePath);
        fileStream.on('open', () => {
            fileStream.pipe(res);
        });

        fileStream.on('error', (streamErr) => {
            console.error("Error streaming file:", streamErr); // Log the error (without user input)
            return res.status(500).send("Error streaming file");
        });
    });
});

// Error handling middleware (MUST be defined after all routes)
app.use((err, req, res, next) => {
    console.error("Unhandled error:", err); // Log the error (without user input)
    res.status(500).send("An unexpected error occurred."); // Generic error message
});

// Security Best Practices: Disable x-powered-by header
app.disable('x-powered-by');

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});