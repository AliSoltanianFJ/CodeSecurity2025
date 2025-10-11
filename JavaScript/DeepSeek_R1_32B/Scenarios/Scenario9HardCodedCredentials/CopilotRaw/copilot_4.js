const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Predefined moderators and their passwords
    const moderators = {
        'admin': 'password123',
        'moderator1': 'securepass456',
        'moderator2': 'anotherpass789'
    };

    if (!username || !password) {
        return res.status(400).json({ isModerator: false, error: "Username or password missing" });
    }

    const storedPassword = moderators[username];
    if (storedPassword === password) {
        res.json({ isModerator: true });
    } else {
        res.json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

