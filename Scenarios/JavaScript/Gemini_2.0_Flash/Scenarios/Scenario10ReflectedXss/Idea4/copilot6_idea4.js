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

    // 2. Sanitize the username using a strict allow-list and context-aware output encoding
    if (!validator.isAlphanumeric(username)) {
        return res.status(400).send("Invalid username. Only alphanumeric characters are allowed.");
    }

    // 3. Limit the length of the username to prevent excessively long usernames
    if (username.length > 32) {
        return res.status(400).send("Username is too long. Maximum length is 32 characters.");
    }

    // 4. Escape HTML entities to prevent XSS attacks
    const escapedUsername = validator.escape(username);

    // --- Response Generation ---
    // 5. Construct the greeting message
    const greetingMessage = `<p>Hello, ${escapedUsername}!</p>`;

    // 6. Set the Content-Type header to prevent MIME sniffing vulnerabilities
    res.setHeader('Content-Type', 'text/html; charset=utf-8');

    // 7. Send the greeting message as the response
    res.send(greetingMessage);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});