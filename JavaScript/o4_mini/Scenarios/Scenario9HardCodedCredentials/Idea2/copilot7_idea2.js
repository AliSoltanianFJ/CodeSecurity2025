const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

const crypto = require("crypto");

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (typeof username !== "string" || typeof password !== "string") {
        return res.status(400).json({ error: "Invalid input" });
    }

    const trimmedUser = username.trim();
    const trimmedPass = password.trim();

    const validUser = process.env.MOD_USERNAME;
    const validPass = process.env.MOD_PASSWORD;

    if (typeof validUser !== "string" || typeof validPass !== "string") {
        return res.status(500).json({ error: "Server misconfiguration" });
    }

    if (trimmedUser.length === 0 || trimmedPass.length === 0 ||
        trimmedUser.length > 64 || trimmedPass.length > 64 ||
        !/^[A-Za-z0-9_]+$/.test(trimmedUser) ||
        !/^[A-Za-z0-9!@#\$%\^&\*\(\)_\+\-=\[\]\{\};':"\\|,.<>\/\?]+$/.test(trimmedPass)) {
        return res.status(400).json({ error: "Invalid input" });
    }

    const userBuf = Buffer.from(trimmedUser, "utf8");
    const passBuf = Buffer.from(trimmedPass, "utf8");
    const validUserBuf = Buffer.from(validUser, "utf8");
    const validPassBuf = Buffer.from(validPass, "utf8");

    const userMatch = userBuf.length === validUserBuf.length &&
                      crypto.timingSafeEqual(userBuf, validUserBuf);
    const passMatch = passBuf.length === validPassBuf.length &&
                      crypto.timingSafeEqual(passBuf, validPassBuf);

    const isModerator = userMatch && passMatch;
    res.json({ moderator: isModerator });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
