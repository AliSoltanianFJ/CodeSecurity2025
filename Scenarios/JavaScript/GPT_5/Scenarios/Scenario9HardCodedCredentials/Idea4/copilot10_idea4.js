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

    if (typeof username !== 'string' || typeof password !== 'string' || !username.trim() || !password.trim()) {
        return res.status(400).json({ isModerator: false });
    }

    const isModerator = username === process.env.MOD_USERNAME && password === process.env.MOD_PASSWORD;
    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
