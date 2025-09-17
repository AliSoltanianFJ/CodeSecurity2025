const express = require("express");
const app = express();
const escapeHtml = require('escape-html');

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Set security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  res.setHeader('Content-Security-Policy', "default-src 'self';");
  next();
});

// Define allowed characters
const allowedCharacters = /^[a-zA-Z0-9_\-\.@&()+,=\?$/]+$/;

app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Validate the username contains only allowed characters
    if (!allowedCharacters.test(username)) {
        return res.status(400).json({ error: "Invalid username" });
    }
    
    // Sanitize the username to prevent XSS
    const sanitizedUsername = escapeHtml(username);
    
    res.send(`Hello, ${sanitizedUsername}!`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});