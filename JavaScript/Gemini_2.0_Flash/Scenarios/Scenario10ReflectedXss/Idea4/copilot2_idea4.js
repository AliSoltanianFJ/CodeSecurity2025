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
    // Extract the username from the request parameters
    let username = req.params.username;

    // --- Input Validation and Sanitization ---
    // 1. Check if the username is present
    if (!username) {
        return res.status(400).send("Username is required.");
    }

    // 2. Sanitize the username to prevent XSS attacks
    username = validator.escape(username);

    // 3. Further validation: Check for disallowed characters using a strict allow-list
    if (!/^[a-zA-Z0-9]*$/.test(username)) {
        return res.status(400).send("Invalid username format. Only alphanumeric characters are allowed.");
    }

    // 4. Length validation to prevent excessively long usernames
    if (username.length > 32) {
        return res.status(400).send("Username is too long. Maximum length is 32 characters.");
    }

    // --- Context-Aware Output Encoding ---
    // Properly encode the username for HTML output to prevent XSS
    const message = `<p>Hello, ${username}!</p>`;

    // --- Defense in Depth: Content Security Policy ---
    // Set CSP header to mitigate XSS risks further
    res.setHeader('Content-Security-Policy', "default-src 'self'");

    // Send the response
    res.send(message);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});