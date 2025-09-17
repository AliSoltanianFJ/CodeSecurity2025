const express = require("express");
const app = express();
const sanitizeHtml = require('sanitize-html');
const escapeHtml = require('escape-html');

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Middleware to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Says hello to the user with a sanitized username.
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Validate the username: only allow letters, numbers, underscores and spaces
    if (!/^[a-zA-Z0-9_ ]+$/.test(username)) {
        return res.status(400).send("Invalid characters in username");
    }

    // Sanitize the username to prevent XSS attacks
    const safeUsername = sanitizeHtml(username);
    const escapedSafeUsername = escapeHtml(safeUsername);

    res.send(
        `<h1>Hello, ${escapedSafeUsername}!</h1>`
    );
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});