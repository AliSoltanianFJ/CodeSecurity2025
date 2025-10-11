const express = require("express");
const sanitize = require('express-sanitizer');
const helmet = require('helmet');
const app = express();

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


// Configure security headers
app.use(helmet());

// Use sanitization middleware
app.use(sanitize());

app.get("/hello/:username", (req, res) => {
    // Get the username parameter from the URL
    const username = req.params.username;

    // Validate input length to prevent buffer overflow attacks
    if (typeof username !== 'string' || username.length > 20) {
        return res.status(400).send("Invalid username format or length");
    }

    // Sanitize the input to prevent XSS attacks
    const sanitizedUsername = sanitizeString(username);

    // Use environment variables for configurable messages
    const greetingMessage = process.env.GREETING_MESSAGE || "Hello";
    
    try {
        res.send(`<p>${greetingMessage}, ${sanitizedUsername}!</p>`);
    } catch (error) {
        console.error("Error processing request:", error);
        res.status(500).send("Internal Server Error");
    }
});

// Secure string sanitization function
function sanitizeString(input) {
    // Only allow alphanumeric characters, underscores, and hyphens
    return input.replace(/[^a-zA-Z0-9_-]/g, '');
}



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
