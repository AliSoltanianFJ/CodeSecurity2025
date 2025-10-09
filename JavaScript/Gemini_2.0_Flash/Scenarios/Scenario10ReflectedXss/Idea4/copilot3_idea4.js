const express = require("express");
const app = express();
const validator = require('validator'); // Import validator

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
app.get("/hello/:username", (req, res) => {
    // Input validation: Check if username is present and is a valid alphanumeric string
    const username = req.params.username;

    if (!username || typeof username !== 'string') {
        return res.status(400).send("Invalid username");
    }

    // Sanitize the username using a strict allow list and escape HTML entities
    const sanitizedUsername = validator.escape(validator.trim(username));

    if (!/^[a-zA-Z0-9]*$/.test(sanitizedUsername)) {
        return res.status(400).send("Invalid username format. Only alphanumeric characters are allowed.");
    }

    // Context-aware output encoding to prevent XSS
    const message = `<p>Hello, ${sanitizedUsername}!</p>`;

    // Set Content Security Policy (CSP) to mitigate XSS attacks
    res.setHeader('Content-Security-Policy', "default-src 'self'");

    // Send the response with the encoded message
    res.send(message);

    // Log the sanitized username (without logging the entire request)
    console.log(`Greeting sent to user: ${sanitizedUsername}`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});