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

    if (!username || !password || typeof username !== 'string' || typeof password !== 'string' || username.length > 100 || password.length > 100) {
        return res.status(400).json({ isModerator: false });
    }

    if (username === process.env.MOD_USERNAME && password === process.env.MOD_PASSWORD) {
        res.json({ isModerator: true });
    } else {
        res.json({ isModerator: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
