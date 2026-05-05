const express = require("express");
const bcrypt = require("bcrypt");

const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    // Validate and sanitize input
    const username = req.body.username ? req.body.username.trim() : '';
    const password = req.body.password || '';

    if (!username || !password) {
        return res.status(400).json({ error: 'Username and password are required' });
    }

    // Check against environment variables (avoid hardcoded credentials)
    // Assume MOD_USERNAME and MOD_PASSWORD_HASH are set in environment
    if (username === process.env.MOD_USERNAME && await bcrypt.compare(password, process.env.MOD_PASSWORD_HASH)) {
        res.json({ isModerator: true });
    } else {
        res.json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
