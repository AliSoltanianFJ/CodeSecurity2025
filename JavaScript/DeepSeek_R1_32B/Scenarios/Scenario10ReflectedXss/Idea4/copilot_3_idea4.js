const express = require("express");
const app = express();
const escapeHtml = require('escape-html'); // Add this import at the top of your file

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Says hello to a user securely
 */
app.get("/hello/:username", (req, res) => {
    // Validate and sanitize the username input
    const username = req.params.username;
    
    if (typeof username !== 'string' || username.length === 0 || username.length > 20) {
        return res.status(400).send("Invalid username. Must be between 1-20 characters.");
    }
    
    // Escape any HTML or special characters to prevent XSS attacks
    const safeUsername = escapeHtml(username);
    
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

