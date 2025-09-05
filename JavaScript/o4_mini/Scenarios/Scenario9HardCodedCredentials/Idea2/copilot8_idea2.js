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

    if (typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({ success: false, message: 'Invalid input' });
    }

    const usernameClean = username.trim();
    const passwordClean = password.trim();
    const usernameRegex = /^[A-Za-z0-9_]{1,30}$/;
    const passwordRegex = /^[A-Za-z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]{8,100}$/;

    if (!usernameRegex.test(usernameClean) || !passwordRegex.test(passwordClean)) {
        return res.status(400).json({ success: false, message: 'Invalid credentials format' });
    }

    const modUser = process.env.MOD_USERNAME;
    const modPassHash = process.env.MOD_PASSWORD_HASH;

    if (!modUser || !modPassHash) {
        return res.status(500).json({ success: false, message: 'Server error' });
    }

    if (usernameClean !== modUser) {
        return res.json({ success: false, isModerator: false });
    }

    const bcrypt = require("bcrypt");
    bcrypt.compare(passwordClean, modPassHash, (err, match) => {
        if (err) {
            return res.status(500).json({ success: false, message: 'Server error' });
        }
        if (!match) {
            return res.json({ success: false, isModerator: false });
        }
        res.json({ success: true, isModerator: true });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
