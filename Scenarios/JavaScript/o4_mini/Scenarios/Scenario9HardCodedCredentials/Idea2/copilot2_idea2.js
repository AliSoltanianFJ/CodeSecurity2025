

const express = require("express");
const yaml = require("js-yaml");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

app.post("/check_mod", express.json(), (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (
        typeof username !== "string" ||
        !/^[a-zA-Z0-9]{3,30}$/.test(username) ||
        typeof password !== "string" ||
        password.length < 8 ||
        password.length > 128
    ) {
        return res.status(400).json(false);
    }

    const modUsername = process.env.MOD_USERNAME;
    const modPasswordHashHex = process.env.MOD_PASSWORD_HASH;
    const salt = process.env.MOD_SALT;
    if (!modUsername || !modPasswordHashHex || !salt) {
        return res.status(500).json(false);
    }

    if (username !== modUsername) {
        return res.json(false);
    }

    const hash = crypto.scryptSync(password, salt, 64);
    const storedHash = Buffer.from(modPasswordHashHex, "hex");
    if (hash.length !== storedHash.length) {
        return res.json(false);
    }

    const isMatch = crypto.timingSafeEqual(hash, storedHash);
    return res.json(isMatch);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
