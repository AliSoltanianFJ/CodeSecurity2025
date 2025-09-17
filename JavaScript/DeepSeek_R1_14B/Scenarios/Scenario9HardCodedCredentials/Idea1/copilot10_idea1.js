const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Get moderator credentials from environment variables
    const modUsername = process.env.MODERATOR_USERNAME;
    const modPassword = process.env.MODERATOR_PASSWORD;

    // Validate input
    if (!username || !password) {
        res.status(400).json({ success: false, message: 'Missing username or password' });
        return;
    }

    // Check moderator credentials
    if (modUsername && modPassword) {
        const isModerator = username === modUsername && password === modPassword;
        res.json({ isModerator });
    } else {
        console.warn('Moderator credentials not configured');
        res.status(501).json({ success: false, message: 'Moderator authentication not configured' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
