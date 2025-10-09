const express = require("express");
const path = require("path");
const crypto = require("crypto"); // For generating secure filenames
const fs = require("fs"); // For file system operations with extra checks

const app = express();

// Security Headers - Defense in Depth
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // Strict CSP
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

// Rate Limiting - Mitigate DoS attacks
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // limit each IP to 100 requests per windowMs
    message: "Too many requests from this IP, please try again after 15 minutes"
});
app.use(limiter);

// Input Validation - Strict and comprehensive
function isValidFilename(filename) {
    if (!filename) return false;
    if (typeof filename !== 'string') return false;
    if (filename.length > 64) return false; // Limit filename length
    if (!/^[a-zA-Z0-9._-]+$/.test(filename)) return false; // Strict allowlist
    return true;
}

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const filename = req.query.name;

    // Input Validation - First line of defense
    if (!isValidFilename(filename)) {
        console.error("Invalid filename:", filename); // Log the invalid input (without user data)
        return res.status(400).send("Invalid filename"); // Generic error message
    }

    // Secure Filename Generation - Mitigate path traversal
    const sanitizedFilename = path.basename(filename); // Remove any path elements
    const imagesDirectory = path.resolve(__dirname, 'images'); // Absolute path to images directory
    const filePath = path.join(imagesDirectory, sanitizedFilename); // Construct the full file path

    // Check if file exists within the allowed directory - Path Traversal Prevention
    if (!filePath.startsWith(imagesDirectory)) {
        console.error("Attempted path traversal:", filePath);
        return res.status(400).send("Invalid request");
    }

    // Enhanced File Existence Check - Prevent TOCTOU issues and ensure file is readable
    fs.access(filePath, fs.constants.R_OK, (err) => {
        if (err) {
            console.error("File not found or not readable:", filePath);
            return res.status(404).send("File not found"); // Generic error message
        }

        // Read the file and send it as a response
        fs.readFile(filePath, (err, data) => {
            if (err) {
                console.error("Error reading file:", filePath, err);
                return res.status(500).send("Internal Server Error"); // Generic error message
            }

            // Determine Content Type - Prevent MIME sniffing vulnerabilities
            const mimeType = getMimeType(filePath);
            res.setHeader('Content-Type', mimeType);

            // Set Cache Headers - Prevent caching of sensitive data
            res.setHeader('Cache-Control', 'no-store');

            // Send the file data
            res.send(data);
        });
    });
});

// MIME Type Validation - Ensure only allowed types are served
function getMimeType(filePath) {
    const ext = path.extname(filePath).toLowerCase();
    const mimeTypes = {
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.gif': 'image/gif',
    };
    const mimeType = mimeTypes[ext];
    if (!mimeType) {
        return 'application/octet-stream'; // Default to safest option
    }
    return mimeType;
}

// Error Handling Middleware - Centralized error logging
app.use((err, req, res, next) => {
    console.error("Unhandled error:", err); // Log the error (without user data)
    res.status(500).send("Internal Server Error"); // Generic error message
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});