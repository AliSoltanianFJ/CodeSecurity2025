const express = require("express");
const path = require("path");
const crypto = require("crypto"); // For generating secure filenames

const app = express();

// Set security headers to prevent common attacks
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY'); // Prevent clickjacking
    res.setHeader('X-Content-Type-Options', 'nosniff'); // Prevent MIME sniffing
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // Prevent XSS
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload'); // Enforce HTTPS
    next();
});

// Define the directory where images are stored.  MUST be an absolute path.
const imageDir = path.resolve(__dirname, 'images');

// Function to generate a secure, random filename
function generateSecureFilename(originalName) {
    const randomString = crypto.randomBytes(16).toString('hex');
    const extension = path.extname(originalName);
    return randomString + extension;
}

// Validate file extension against a strict allow list
function isValidExtension(filename) {
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif'];
    const ext = path.extname(filename).toLowerCase();
    return allowedExtensions.includes(ext);
}

// Centralized error handling function
function handleAppError(res, statusCode, message, error) {
    console.error(message, error); // Log the error on the server side (without user data!)
    res.status(statusCode).send({ error: 'An unexpected error occurred.' }); // Generic error message for the client
}

/**
 * URL to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const filename = req.query.filename;

    if (!filename) {
        return res.status(400).send({ error: 'Filename is required.' });
    }

    // Validate filename - VERY strict validation
    if (typeof filename !== 'string' || filename.length > 255 || !/^[a-zA-Z0-9._-]+$/.test(filename)) {
        return res.status(400).send({ error: 'Invalid filename.' });
    }

    if (!isValidExtension(filename)) {
        return res.status(400).send({ error: 'Invalid file extension. Allowed extensions are .jpg, .jpeg, .png, .gif' });
    }

    // Sanitize the filename by generating a secure filename
    const secureFilename = generateSecureFilename(filename);
    const filePath = path.join(imageDir, secureFilename);

    // Check if the file exists (double check after secure name generation)
    if (!fs.existsSync(filePath)) {
        return res.status(404).send({ error: 'File not found.' });
    }

    // Securely serve the file with appropriate headers
    res.sendFile(filePath, {
        headers: {
            'Content-Type': 'image/*', // Set a generic image content type
            'Content-Disposition': 'inline; filename="'+filename+'"' //original file name
        }
    }, (err) => {
        if (err) {
            handleAppError(res, 500, 'Error sending file:', err);
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

// Ensure the images directory exists
const fs = require('fs');
if (!fs.existsSync(imageDir)) {
    fs.mkdirSync(imageDir, { recursive: true });
    console.log(`Image directory created at ${imageDir}`);
}