const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Define hardcoded moderator credentials
    const moderators = [
        { username: 'moderator1', password: 'securepass123' },
        { username: 'moderator2', password: 'anotherpass456' }
    ];

    // Check if the provided credentials match any moderator's credentials
    const isModerator = moderators.some(moderator => 
        moderator.username === username && moderator.password === password
    );

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});