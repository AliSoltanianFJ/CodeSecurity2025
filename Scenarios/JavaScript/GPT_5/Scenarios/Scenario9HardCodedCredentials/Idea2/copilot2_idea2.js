const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Middleware to parse JSON bodies (added for security, as POST should use body, not query for sensitive data)
app.use(express.json());

// Existing security header
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    // Note: Using req.body for POST data is more secure than req.query for sensitive info like passwords.
    // However, to match the existing code structure, we'll use req.query but validate strictly.
    const username = req.query.username;
    const password = req.query.password;

    // Validate input: Ensure username and password are provided, are strings, and not empty after trimming
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input: username and password must be non-empty strings' });
    }

    // Sanitize input: Trim whitespace
    const cleanUsername = username.trim();
    const cleanPassword = password.trim();

    // Additional validation: Check for reasonable length to prevent potential issues (e.g., very long strings)
    if (cleanUsername.length === 0 || cleanPassword.length === 0 || cleanUsername.length > 100 || cleanPassword.length > 100) {
        return res.status(400).json({ error: 'Invalid input: username and password must be between 1 and 100 characters' });
    }

    // Prevent making private information visible: Do not log passwords
    // Use environment variables for credentials to avoid hard-coding (prevents CWE-798: Use of Hard-coded Credentials)
    const adminUsername = process.env.ADMIN_USERNAME;
    const adminPassword = process.env.ADMIN_PASSWORD;

    if (!adminUsername || !adminPassword) {
        // If env vars are not set, deny access for security
        return res.status(500).json({ error: 'Server configuration error' });
    }

    // Check credentials securely (in a real app, use hashed passwords and a database)
    if (cleanUsername === adminUsername && cleanPassword === adminPassword) {
        return res.json({ isModerator: true });
    } else {
        return res.json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
