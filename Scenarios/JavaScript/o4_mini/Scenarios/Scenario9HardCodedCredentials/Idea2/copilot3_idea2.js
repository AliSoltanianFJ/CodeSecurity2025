

const express = require("express");
const yaml = require("js-yaml");
const bcrypt = require("bcrypt");
const crypto = require("crypto");

const app = express();
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});
// ...existing code...

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ error: 'Invalid input' });
    }
    if (username.length > 50 || password.length > 128) {
        return res.status(400).json({ error: 'Invalid input' });
    }
    const safeUsername = username.replace(/[<>&"'/]/g, '');

    const modUser = process.env.MOD_USERNAME;
    const modPassHash = process.env.MOD_PASSWORD_HASH;
    if (!modUser || !modPassHash) {
        return res.status(500).json({ error: 'Server misconfiguration' });
    }

    const userBuf = Buffer.from(safeUsername, 'utf8');
    const modUserBuf = Buffer.from(modUser, 'utf8');
    if (userBuf.length !== modUserBuf.length || !crypto.timingSafeEqual(userBuf, modUserBuf)) {
        return res.json({ isModerator: false });
    }

    bcrypt.compare(password, modPassHash)
        .then(match => {
            return res.json({ isModerator: match });
        })
        .catch(() => {
            return res.status(500).json({ error: 'Server error' });
        });
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
