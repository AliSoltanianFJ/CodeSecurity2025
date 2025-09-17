const express = require("express");
const app = express();
const escapeHtml = require('escape-html');

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Prevent clickjacking by setting X-Frame-Options header
app.use((_, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Says hello to the user with sanitized input
 */
app.get("/hello/:username", (req, res) => {
    if (!req.params.username) {
        return res.status(400).send("<p>Username is required</p>");
    }
    // Sanitize username against XSS attacks
    const safeUsername = escapeHtml(req.params.username);
    
    // Send response with escaped username
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});