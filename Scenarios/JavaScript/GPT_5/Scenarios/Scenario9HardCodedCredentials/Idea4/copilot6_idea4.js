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

    if (typeof username !== 'string' || typeof password !== 'string' || username.length === 0 || password.length === 0 || username.length > 100 || password.length > 100) {
        return res.status(400).json({ isModerator: false });
    }

    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    if (!modUsername || !modPassword) {
        return res.status(500).json({ isModerator: false });
    }

    if (username === modUsername && password === modPassword) {
        return res.json({ isModerator: true });
    } else {
        return res.status(401).json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
