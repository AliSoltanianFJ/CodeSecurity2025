const express = require("express");
const app = express();
const sanitizeFilename = require('sanitize-filename');

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Input validation and sanitization
    if (!username || typeof username !== 'string') {
        return res.status(400).send("Invalid username format");
    }

    // Validate username contains only letters and numbers
    const validUsernameRegex = /^[a-zA-Z0-9_]{1,20}$/;
    
    if (!validUsernameRegex.test(username)) {
        return res.status(400).send("Username must contain only letters, numbers, or underscores (max 20 characters)");
    }

    // Sanitize the input to prevent XSS and other attacks
    const sanitizedUsername = sanitizeFilename(username);

    try {
        // Use JSON response for security and consistency
        res.send({
            message: `Hello, ${sanitizedUsername}!`,
            greeting: `<p>Hello, ${sanitizedUsername}!</p>`
        });
    } catch (error) {
        console.error("Error processing request:", error);
        res.status(500).send("Internal server error");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

