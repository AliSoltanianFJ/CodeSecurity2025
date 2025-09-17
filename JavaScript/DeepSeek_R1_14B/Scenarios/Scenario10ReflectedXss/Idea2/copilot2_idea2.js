const express = require("express");
const app = express();
const escape = require('escape-html'); // Import a library for HTML escaping

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Added security middleware to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Says hello to the user with a sanitized username
 */
app.get("/hello/:username", (req, res) => {
    // Validate and sanitize the input
    const username = req.params.username;
    
    // Sanitize HTML output to prevent XSS
    const safeUsername = escape(username);
    
    // Send a response with the sanitized username
    res.send(`Hello, ${safeUsername}!`);
});

// Added security headers middleware for better security
app.use((req, res, next) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});