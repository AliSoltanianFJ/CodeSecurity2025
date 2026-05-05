const express = require("express");
const app = express();
const validator = require('validator'); // Import validator
const helmet = require('helmet'); // Import helmet

// Apply helmet middleware for setting secure HTTP headers
app.use(helmet());

// Enable the XSS filter
app.use(helmet.xssFilter());

// Prevent MIME-sniffing vulnerabilities
app.use(helmet.noSniff());

// Implement a CSP (Content Security Policy)
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'", "'unsafe-inline'"], // Consider using nonces or hashes instead of 'unsafe-inline'
    styleSrc: ["'self'", "'unsafe-inline'"],
    imgSrc: ["'self'", "data:"],
    fontSrc: ["'self'"],
    connectSrc: ["'self'"],
  },
}));

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Centralized logging function (using console.error for simplicity, but a proper logging library is recommended)
function logError(message, error) {
    console.error(message, error);
    // In a real application, use a logging library to log to a file or external service
    // Ensure that user-provided data is NOT included in the log messages to prevent information leakage
}

// Rate limiting middleware to prevent abuse
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per windowMs
  message: "Too many requests from this IP, please try again after 15 minutes"
});

app.use(limiter);

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // Input validation and sanitization
    let username = req.params.username;

    // Check if username is defined
    if (!username) {
        logError("Username is undefined", new Error("Missing username"));
        return res.status(400).send("Error: Username is required.");
    }

    // Sanitize the username using a strict allow list and escaping
    if (typeof username !== 'string') {
        logError("Invalid username format", new Error("Non-string username provided"));
        return res.status(400).send("Invalid username format.");
    }

    // Validate username length
    if (username.length < 3 || username.length > 20) {
        logError("Invalid username length", new Error("Username length out of bounds"));
        return res.status(400).send("Invalid username length. Must be between 3 and 20 characters.");
    }

    // Use validator to ensure the username contains only alphanumeric characters
    if (!validator.isAlphanumeric(username)) {
        logError("Invalid username format", new Error("Username contains invalid characters"));
        return res.status(400).send("Invalid username format. Only alphanumeric characters are allowed.");
    }

    // Context-aware output encoding to prevent XSS
    const encodedUsername = validator.escape(username);

    // Construct the response using template literals with encoded data
    const responseMessage = `<p>Hello, ${encodedUsername}!</p>`;

    // Set Content Security Policy to mitigate XSS attacks
    res.setHeader('Content-Security-Policy', "default-src 'self'");

    // Send the response
    res.send(responseMessage);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});