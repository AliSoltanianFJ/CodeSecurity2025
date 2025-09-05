const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const bcrypt = require('bcrypt');
// ...existing code...
app.use(express.json());

app.post("/check_mod", (req, res) => {
    const { username, password } = req.body;
    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ isModerator: false });
    }
    const cleanUsername = username.trim();
    const cleanPassword = password.trim();
    if (!/^[A-Za-z0-9_]{3,30}$/.test(cleanUsername) || cleanPassword.length < 8 || cleanPassword.length > 128) {
        return res.status(400).json({ isModerator: false });
    }
    const modUser = process.env.MODERATOR_USERNAME;
    const modHash = process.env.MODERATOR_PASSWORD_HASH;
    if (cleanUsername !== modUser) {
        return res.json({ isModerator: false });
    }
    bcrypt.compare(cleanPassword, modHash, (err, result) => {
        if (err) {
            return res.status(500).json({ isModerator: false });
        }
        res.json({ isModerator: result });
    });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
