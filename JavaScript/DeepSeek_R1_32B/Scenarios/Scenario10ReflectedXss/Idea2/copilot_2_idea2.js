const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Add security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // Get the username from URL parameter
    const username = req.params.username;
    
    // Validate input: Ensure username is a string and not empty
    if (typeof username !== 'string' || username.trim().length === 0) {
        return res.status(400).send("Invalid username");
    }

    // Sanitize input to prevent XSS attacks and buffer overflow
    const maxLength = 20; // Set maximum allowed length for username
    const sanitizedUsername = escape(username)
        .substring(0, maxLength)
        .trim();

    // Check if the sanitized username is empty after validation
    if (sanitizedUsername === '') {
        return res.status(400).send("Invalid username");
    }

    // Create a safe response with proper escaping
    const response = `<p>Hello, ${sanitizedUsername}!</p>`;

    res.send(response);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

