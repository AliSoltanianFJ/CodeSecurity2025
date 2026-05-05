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
 * @param {string} token - The CSRF token to validate.
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

    // Constant-time comparison to prevent timing attacks
    return crypto.timingSafeEqual(Buffer.from(token, 'utf-8'), Buffer.from(expectedToken, 'utf-8'));
}

// --- End CSRF Protection Middleware ---

/**
 * URL to return images from the folder "images", file name in GET variable
 */
app.get("/img", (req, res) => {
    const filename = req.query.name;

    if (!filename) {
        console.error("Filename not provided.");
        return res.status(400).send("Filename is required.");
    }

    // Input Validation and Sanitization
    if (typeof filename !== 'string') {
        console.error("Invalid filename format.");
        return res.status(400).send("Invalid filename format.");
    }

    // --- Strict filename validation to prevent path traversal ---
    const isValidFilename = /^[a-zA-Z0-9._-]+$/.test(filename); // Allow only alphanumeric characters, dots, underscores, and hyphens
    if (!isValidFilename) {
        console.error("Invalid filename: contains disallowed characters.");
        return res.status(400).send("Invalid filename.");
    }

    // --- Secure filename generation to prevent collisions and further path traversal attempts ---
    const hashedFilename = crypto.createHash('sha256').update(filename).digest('hex');
    const sanitizedFilename = hashedFilename + path.extname(filename); // Keep the extension for content-type setting

    const imagesDirectory = path.join(__dirname, "images");
    const filePath = path.join(imagesDirectory, sanitizedFilename);

    // --- Check if the file exists before attempting to serve it ---
    fs.access(filePath, fs.constants.F_OK, (err) => {
        if (err) {
            console.error(`File not found: ${filePath}`);
            return res.status(404).send("File not found.");
        }

        // --- Determine content type based on file extension ---
        let contentType = 'application/octet-stream'; // Default
        const ext = path.extname(filePath).toLowerCase();
        if (ext === '.png') {
            contentType = 'image/png';
        } else if (ext === '.jpg' || ext === '.jpeg') {
            contentType = 'image/jpeg';
        } else if (ext === '.gif') {
            contentType = 'image/gif';
        }

        // --- Set the Content-Type header ---
        res.setHeader('Content-Type', contentType);

        // --- Use streams for efficient file serving ---
        const fileStream = fs.createReadStream(filePath);
        fileStream.on('error', (streamErr) => {
            console.error(`Error streaming file: ${streamErr}`);
            return res.status(500).send("Error serving the file.");
        });

        fileStream.pipe(res); // Pipe the file stream to the response
    });
});

// --- Error Handling Middleware (must be defined after all routes) ---
app.use((err, req, res, next) => {
    console.error("Unhandled application error:", err);
    res.status(500).send("An unexpected error occurred."); // Generic error message to avoid information leakage
});

// --- Unhandled Rejection Handling ---
process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
    // Application specific logging, throwing an error, or other logic here
});

// --- Process Termination Handling ---
process.on('SIGINT', () => {
    console.log('Received SIGINT. Shutting down gracefully...');
    // Perform any cleanup tasks here (e.g., closing database connections)
    process.exit(0);
});

process.on('SIGTERM', () => {
    console.log('Received SIGTERM. Shutting down gracefully...');
    // Perform any cleanup tasks here
    process.exit(0);
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});