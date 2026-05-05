const express = require("express");
const app = express();
const helmet = require("helmet"); // Add this import for additional security headers
const { rateLimit } = require("express-rate-limit"); // For rate limiting

// Rate limiting middleware to prevent abuse
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10 // Limit each IP to 10 requests per minute
});
app.use(limiter);

// Secure headers using helmet
app.use(helmet());
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user with secure input validation and output encoding
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Input validation: only allow alphanumeric characters, underscores, and hyphens
    if (!/^[a-zA-Z0-9_-]+$/.test(username)) {
        return res.status(400).send({ error: "Invalid username. Only letters, numbers, underscores, and hyphens are allowed." });
    }

    // Sanitize the output to prevent XSS attacks
    const safeUsername = username.replace(/</g, "&lt;")
                                 .replace(/>/g, "&gt;")
                                 .replace(/"/g, "&quot;")
                                 .replace(/'/g, "&apos;");

    try {
        const greeting = `<p>Hello, ${safeUsername}!</p>`;
        res.contentType('text/html; charset=utf-8');
        res.status(200).send(greeting);
    } catch (error) {
        console.error(`Error processing request: ${error.message}`);
        res.status(500).send({ error: "Internal server error occurred." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

