const express = require("express");
const app = express();
const helmet = require("helmet");
const crypto = require('crypto');

// Apply security middleware
app.use(helmet());

// Set CSP headers
app.use(helmet.contentSecurityPolicy({
  directives: {
    defaultSrc: ["'self'"],
    scriptSrc: ["'self'"], // Consider adding a nonce or hash for inline scripts
    styleSrc: ["'self'", "'unsafe-inline'"], // Be cautious with 'unsafe-inline'
    imgSrc: ["'self'", "data:"],
    connectSrc: ["'self'"],
    fontSrc: ["'self'"],
    objectSrc: ["'none'"],
    mediaSrc: ["'self'"],
    frameSrc: ["'none'"], // Prevent clickjacking
  },
}));

// Prevent MIME sniffing
app.use(helmet.noSniff());

// Prevent XSS filtering (already covered by proper output encoding, but kept for defense in depth)
app.use(helmet.xssFilter());

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Prevent Clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Input Validation Function
function isValidUsername(username) {
    if (!username) return false;
    if (typeof username !== 'string') return false;
    // Check length
    if (username.length < 3 || username.length > 20) return false;
    // Only allow alphanumeric characters
    if (!/^[a-zA-Z0-9]*$/.test(username)) return false;
    return true;
}

// Output Encoding Function
function encodeOutput(str) {
    if (!str) return '';
    let encoded = '';
    for (let i = 0; i < str.length; i++) {
        const charCode = str.charCodeAt(i);
        encoded += '&#' + charCode + ';';
    }
    return encoded;
}

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;

    if (!isValidUsername(username)) {
        console.error("Invalid username provided:", username);
        return res.status(400).send("Invalid username. Only alphanumeric characters between 3 and 20 are allowed.");
    }

    const encodedUsername = encodeOutput(username);
    const message = `<p>Hello, ${encodedUsername}!</p>`;
    res.send(message);
});

// Error handling middleware (must be defined after all other routes)
app.use((err, req, res, next) => {
    console.error("Server error:", err);
    res.status(500).send("An unexpected error occurred.");
});

// Rate limiting to prevent abuse
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 100, // Limit each IP to 100 requests per windowMs
    message: "Too many requests from this IP, please try again after 15 minutes"
});

app.use(limiter);

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});