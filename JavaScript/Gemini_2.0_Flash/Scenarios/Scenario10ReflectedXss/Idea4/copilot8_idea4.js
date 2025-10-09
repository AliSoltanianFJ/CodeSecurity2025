const express = require("express");
const app = express();
const validator = require('validator');
const helmet = require('helmet');

// --- existing code ---
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
    const username = req.params.username;

    // Input validation: Check if username is provided
    if (!username) {
        console.error("Error: Username is missing.");
        return res.status(400).send("Error: Username is required.");
    }

    // Input validation: Sanitize and validate the username
    if (!validator.isAlphanumeric(username)) {
        console.error("Error: Invalid username format.");
        return res.status(400).send("Error: Invalid username format. Only alphanumeric characters are allowed.");
    }

    // Input validation: Check username length
    if (username.length < 3 || username.length > 20) {
        console.error("Error: Username length is invalid.");
        return res.status(400).send("Error: Username must be between 3 and 20 characters.");
    }

    // Context-aware output encoding to prevent XSS
    const encodedUsername = validator.escape(username);

    // Securely generate the greeting
    const greeting = `<p>Hello, ${encodedUsername}!</p>`;

    // Set Content Security Policy to mitigate XSS attacks
    res.setHeader(
        'Content-Security-Policy',
        "default-src 'self'; script-src 'none'; object-src 'none'; style-src 'none'"
    );

    // Send the greeting
    res.send(greeting);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});