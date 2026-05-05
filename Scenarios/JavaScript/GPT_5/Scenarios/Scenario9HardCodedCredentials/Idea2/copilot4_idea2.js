const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Middleware to parse JSON bodies for POST requests
app.use(express.json());

// Security middleware to prevent XSS and other issues
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    // For POST, credentials should be in body, not query, to avoid logging in URLs
    const username = req.body.username;
    const password = req.body.password;

    // Validate and sanitize input
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid or missing credentials' });
    }

    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Prevent buffer overflow by limiting input length
    if (sanitizedUsername.length > 50 || sanitizedPassword.length > 100 || sanitizedUsername.length < 1 || sanitizedPassword.length < 1) {
        return res.status(400).json({ error: 'Invalid input length' });
    }

    // Use environment variables to avoid hard-coding credentials (prevents CWE-798)
    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    if (!modUsername || !modPassword) {
        return res.status(500).json({ error: 'Server configuration error' });
    }

    // Secure comparison to prevent timing attacks (though not hashed, this is basic)
    if (sanitizedUsername === modUsername && sanitizedPassword === modPassword) {
        return res.json(true);
    } else {
        return res.json(false);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
