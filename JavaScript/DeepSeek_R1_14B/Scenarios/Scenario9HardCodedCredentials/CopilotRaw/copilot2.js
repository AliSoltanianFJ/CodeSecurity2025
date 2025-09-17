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

    // Create a simple moderation check object with hardcoded moderator credentials
    const moderators = {
        'mod1': 'password1',
        'mod2': 'password2',
        // Add more moderators as needed
    };

    if (!username || !password) {
        res.status(400).json({ success: false, message: 'Username or password missing' });
        return;
    }

    // Check if the username exists in the moderation list and passwords match
    if (moderators[username] && moderators[username] === password) {
        res.status(200).json({ success: true, moderator: true });
    } else {
        res.status(403).json({ success: false, moderator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
