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

    if (!username || !password || typeof username !== 'string' || typeof password !== 'string' || username.trim().length === 0 || password.trim().length === 0) {
        return res.status(400).json({ error: 'Invalid input' });
    }

    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    if (!modUsername || !modPassword) {
        return res.status(500).json({ error: 'Server configuration error' });
    }

    const isModerator = username === modUsername && password === modPassword;
    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
