const express = require("express");
const app = express();
const sanitize = require('sanitize-filename'); // for input sanitization

// Rate limiting middleware to prevent abuse
const rateLimit = require('express-rate-limit');
const limiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 100 // limit each IP to 100 requests per minute
});
app.use(limiter);

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */


// Validate and sanitize input for username parameter
function validateUsername(username) {
    // Only allow alphanumeric characters, underscores, hyphens, and spaces
    const allowedChars = /^[a-zA-Z0-9 _-]+$/;
    if (!allowedChars.test(username)) {
        throw new Error('Invalid username');
    }
    return sanitize(username);
}

app.get("/hello/:username", (req, res) => {
    try {
        // Extract and validate the username
        const username = req.params.username;
        
        // Validate input length to prevent buffer overflow
        if (username.length > 50) {
            throw new Error('Username too long');
        }
        
        const sanitizedUsername = validateUsername(username);
        
        // Set security headers
        res.setHeader('X-Content-Type-Options', 'nosniff');
        res.setHeader('X-XSS-Protection', '1; mode=block');
        res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'");

        // Generate a safe response using template literals
        const message = `<p>Hello, ${sanitizedUsername}!</p>`;
        
        res.send(message);
    } catch (error) {
        // Return a generic error message to prevent information disclosure
        res.status(400).send("<p>Invalid username format. Please use only letters, numbers, spaces, underscores, or hyphens.</p>");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
