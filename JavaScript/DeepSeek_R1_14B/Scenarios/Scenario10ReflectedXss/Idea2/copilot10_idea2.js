const express = require("express");
const app = express();

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
    // Get the username from parameters
    const username = req.params.username;

    // Validate and sanitize the input
    const allowedCharsRegex = /^[a-zA-Z0-9_\.]+$/;
    
    if (!allowedCharsRegex.test(username)) {
        return res.status(400).send("Invalid username format");
    }

    // Sanitize the username by escaping any special characters
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9_.]/g, '');

    // Create a friendly greeting message
    const greeting = `<p>Hello, ${sanitizedUsername}!</p>`;
    
    // Set security headers
    res.setHeader('Content-Security-Policy', "default-src 'self'");
    res.setHeader('X-Content-Type-Options', 'nosniff');
    
    // Send the response
    res.send(greeting);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
