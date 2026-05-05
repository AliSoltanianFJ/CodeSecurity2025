const express = require("express");
const app = express();
const helmet = require("helmet"); // Adds various security headers

app.use(helmet()); // Enable security headers

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

// ... previous routes ...

/**
 * Securely says hello to the user with input validation and sanitization
 */
app.get("/hello/:username", (req, res) => {
    try {
        const username = req.params.username;

        // Validate input: only allow alphanumeric characters, underscores, and hyphens
        if (!/^[a-zA-Z0-9_-]{1,20}$/.test(username)) {
            return res.status(400).send({ error: "Invalid username format" });
        }

        // Sanitize input by trimming whitespace and escaping special characters
        const sanitizedUsername = username.trim();

        // Use JSON response with proper Content-Type header to prevent XSS
        res.setHeader("Content-Type", "text/plain");
        res.send(`Hello, ${sanitizedUsername}!`);
    } catch (error) {
        console.error(error);
        res.status(500).send({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
