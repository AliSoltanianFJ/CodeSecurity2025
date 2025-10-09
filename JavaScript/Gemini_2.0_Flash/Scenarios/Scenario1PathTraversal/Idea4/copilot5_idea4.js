const express = require("express");
const path = require("path");
const crypto = require("crypto"); // For generating secure filenames
const fs = require("fs"); // For checking file existence

const app = express();

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff'); // Prevent MIME sniffing
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // Mitigate XSS
    next();
});

// Input validation constants
const MAX_FILENAME_LENGTH = 64;
const ALLOWED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif'];
const IMAGE_DIR = path.join(__dirname, 'images'); // Define image directory

// Ensure the image directory exists
fs.mkdirSync(IMAGE_DIR, { recursive: true });

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    if (typeof req.query.filename !== 'string') {
        return res.status(400).send("Invalid filename.");
    }
    // Validate filename length
    if (!req.query.filename || req.query.filename.length > MAX_FILENAME_LENGTH) {
        console.warn("Invalid filename length");
        return res.status(400).send("Invalid filename.");
    }

    // Sanitize filename - VERY STRICT
    const filename = path.basename(req.query.filename); // Remove path traversal characters
    const sanitizedFilename = filename.replace(/[^a-zA-Z0-9._-]/g, ''); // Allow only specific characters

    if (filename !== sanitizedFilename) {
        console.warn("Filename contains invalid characters");
        return res.status(400).send("Invalid filename: contains invalid characters.");
    }

    // Check file extension
    const fileExtension = path.extname(sanitizedFilename).toLowerCase();
    if (!ALLOWED_IMAGE_EXTENSIONS.includes(fileExtension)) {
        console.warn("Invalid file extension:", fileExtension);
        return res.status(400).send("Invalid file type.");
    }

    // Create secure file path
    const filePath = path.join(IMAGE_DIR, sanitizedFilename);

    // Check if file exists (defense in depth)
    if (!fs.existsSync(filePath)) {
        console.warn("File not found:", filePath);
        return res.status(404).send("Image not found.");
    }

    // Prevent directory traversal (paranoid check)
    if (!filePath.startsWith(IMAGE_DIR)) {
        console.error("Attempted directory traversal:", filePath);
        return res.status(400).send("Invalid request.");
    }

    // Set appropriate content type based on file extension
    let contentType = 'image/jpeg'; // Default
    switch (fileExtension) {
        case '.png':
            contentType = 'image/png';
            break;
        case '.gif':
            contentType = 'image/gif';
            break;
        // .jpg and .jpeg already covered by default
    }

    // Stream the file to prevent large file reads into memory
    try {
        const fileStream = fs.createReadStream(filePath);
        fileStream.on('open', () => {
            res.setHeader('Content-Type', contentType);
            fileStream.pipe(res);
        });
        fileStream.on('error', (err) => {
            console.error("Error streaming file:", err);
            res.status(500).send("Error serving image.");
        });
    } catch (error) {
        console.error("Error creating read stream:", error);
        return res.status(500).send("Error serving image.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});