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
        return res.status(400).send("Error: Username is required.");
    }

    // 2. Sanitize the username using a strict allow-list and escaping
    if (!validator.isAlphanumeric(username)) {
        return res.status(400).send("Error: Username must be alphanumeric.");
    }

    // 3. Context-aware output encoding to prevent XSS
    const encodedUsername = validator.escape(username);

    // --- Defense in Depth: Additional check for length ---
    if (encodedUsername.length > 50) {
        return res.status(400).send("Error: Username is too long.");
    }

    // --- Secure response to prevent content sniffing ---
    res.setHeader('Content-Type', 'text/html; charset=utf-8');

    // --- Construct the HTML response safely ---
    const htmlResponse = `<p>Hello, ${encodedUsername}!</p>`;

    // --- Send the response ---
    res.send(htmlResponse);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});