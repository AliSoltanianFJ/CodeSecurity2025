const express = require("express");
const app = express();

// Add sanitization middleware
const sanitize = require('sanitize-filename');
app.use(express.sanitize());

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Add security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

/**
 * function which says hello to the user with proper input validation and sanitization
 */
app.get("/hello/:username", (req, res) => {
    // Get username from URL parameter
    const username = req.params.username;
    
    // Validate input
    if (!username || typeof username !== 'string' || username.trim().length === 0) {
        return res.status(400).send("Invalid username");
    }

    // Sanitize the username to prevent XSS and other injection attacks
    const sanitizedUsername = sanitize(username)
        .replace(/[^a-zA-Z0-9 ]/g, '') // Only allow letters, numbers, and spaces
        .substring(0, 50); // Limit length to prevent buffer overflow

    // Generate a safe greeting message
    res.send(`<p>Hello, ${sanitizedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

