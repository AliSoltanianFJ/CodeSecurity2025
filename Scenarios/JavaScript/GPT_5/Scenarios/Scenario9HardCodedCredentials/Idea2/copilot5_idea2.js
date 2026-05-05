const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use(express.json());

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    // Sanitize input
    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    // Validate input
    if (sanitizedUsername.length === 0 || sanitizedPassword.length === 0 || sanitizedUsername.length > 100 || sanitizedPassword.length > 100) {
        return res.status(400).json({ error: 'Invalid credentials' });
    }

    // Check against environment variables (no hard-coding)
    const isModerator = sanitizedUsername === process.env.MOD_USERNAME && sanitizedPassword === process.env.MOD_PASSWORD;

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
