const express = require("express");
const path = require("path");
const crypto = require("crypto"); // For generating secure filenames
const fs = require("fs"); // For checking file existence

const app = express();

// Security Headers - Mitigating various risks
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY'); // Prevents clickjacking
    res.setHeader('X-Content-Type-Options', 'nosniff'); // Mitigates MIME sniffing attacks
    res.setHeader('Content-Security-Policy', "default-src 'self'"); // Prevents loading resources from other domains
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload'); // Enforces HTTPS
    next();
});

// --- CSRF Protection Middleware ---
app.use((req, res, next) => {
    if (req.method === 'POST') {
        const csrfToken = req.headers['csrf-token']; // Or wherever you store it
        if (!csrfToken || !validateCsrfToken(csrfToken, req.sessionID)) { // Assuming session-based CSRF
            console.error('CSRF token validation failed.');
            return res.status(403).send('CSRF token validation failed.');
        }
    }
    next();
});

/**
 * Validates the CSRF token against the session ID.
 * @param {string} token - The CSRF token from the request.
 * @param {string} sessionId - The session ID of the user.
 * @returns {boolean} - True if the token is valid, false otherwise.
 */
function validateCsrfToken(token, sessionId) {
    if (!token || !sessionId) {
        return false;
    }

    const expectedToken = crypto.createHmac('sha256', 'your-secret-key') // Replace 'your-secret-key' with a strong, randomly generated secret
        .update(sessionId)
        .digest('hex');

    return token === expectedToken;
}

/**
 * url to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    // Input sanitization and validation
    const filename = req.query.filename;

    if (!filename) {
        console.error("Filename not provided.");
        return res.status(400).send("Filename is required.");
    }

    // Validate filename:  This regex ensures the filename contains only alphanumeric characters, underscores, and hyphens.
    if (!/^[a-zA-Z0-9_-]+$/.test(filename)) {
        console.error("Invalid filename format.");
        return res.status(400).send("Invalid filename format.");
    }

    // Create a secure file path
    const imagesDirectory = path.join(__dirname, "images"); // Absolute path to the images directory
    const filePath = path.join(imagesDirectory, filename);

    // --- Path traversal prevention ---
    if (!filePath.startsWith(imagesDirectory)) {
        console.error("Attempted path traversal.");
        return res.status(400).send("Invalid file path.");
    }

    // Check if the file exists
    fs.access(filePath, fs.constants.R_OK, (err) => {
        if (err) {
            console.error("File not found or not accessible:", filePath);
            return res.status(404).send("File not found.");
        }

        // Set the correct content type
        const mimeType = getMimeType(filePath);
        res.setHeader("Content-Type", mimeType);

        // --- Stream the file ---
        const fileStream = fs.createReadStream(filePath);
        fileStream.on('error', (streamErr) => {
            console.error("Error streaming file:", streamErr);
            return res.status(500).send("Error streaming file.");
        });
        fileStream.pipe(res); // Send the file to the response

        // --- Logging (without sensitive data) ---
        console.log(`Served image: ${filename}`);
    });
});

/**
 * Determines the MIME type of a file based on its extension.
 * @param {string} filePath - The path to the file.
 * @returns {string} - The MIME type of the file.
 */
function getMimeType(filePath) {
    const ext = path.extname(filePath).toLowerCase();
    switch (ext) {
        case ".jpg":
        case ".jpeg":
            return "image/jpeg";
        case ".png":
            return "image/png";
        case ".gif":
            return "image/gif";
        case ".bmp":
            return "image/bmp";
        case ".webp":
            return "image/webp";
        default:
            return "application/octet-stream"; // Default to binary data
    }
}

// --- Error handling middleware ---
app.use((err, req, res, next) => {
    console.error("Unhandled error:", err);
    res.status(500).send("An unexpected error occurred."); // Generic error message
});

// --- Unhandled Rejection Handling ---
process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
    // Application specific logging, throwing an error, or other logic here
});

// ---  Fallback route ---
app.use((req, res, next) => {
    res.status(404).send("Resource not found.");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});