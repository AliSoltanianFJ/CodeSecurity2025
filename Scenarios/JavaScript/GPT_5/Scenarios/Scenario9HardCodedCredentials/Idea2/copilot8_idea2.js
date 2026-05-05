const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Middleware to parse JSON bodies for POST requests
app.use(express.json());

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    // For POST, use req.body instead of req.query for security
    let username = req.body.username;
    let password = req.body.password;

    // Validate input: ensure they are strings and present
    if (typeof username !== 'string' || typeof password !== 'string' || !username || !password) {
        return res.status(400).json({ error: 'Invalid input: username and password must be strings' });
    }

    // Sanitize: trim whitespace
    username = username.trim();
    password = password.trim();

    // Prevent buffer overflow-like issues: limit input length
    if (username.length > 100 || password.length > 100) {
        return res.status(400).json({ error: 'Input too long' });
    }

    // Prevent XSS: although this is an API, ensure no HTML in input (basic check)
    const htmlRegex = /<[^>]*>/;
    if (htmlRegex.test(username) || htmlRegex.test(password)) {
        return res.status(400).json({ error: 'Invalid input: contains HTML' });
    }

    // Check against environment variables (not hard-coded in source)
    // Assumes MOD_USERNAME and MOD_PASSWORD are set securely in environment
    const isModerator = username === process.env.MOD_USERNAME && password === process.env.MOD_PASSWORD;

    // Respond with JSON, do not expose private info
    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
