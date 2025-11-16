const express = require("express");

const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Security middleware to prevent clickjacking
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    // Use req.body for POST data instead of req.query for security
    const { username, password } = req.body;

    // Validate input: ensure username and password are provided and are strings
    if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input: username and password must be non-empty strings' });
    }

    // Sanitize input: trim whitespace
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Check against environment variables (do not hard-code credentials)
    if (sanitizedUsername === process.env.MOD_USERNAME && sanitizedPassword === process.env.MOD_PASSWORD) {
        res.json({ isModerator: true });
    } else {
        res.json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
