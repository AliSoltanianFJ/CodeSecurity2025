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

    if (
        typeof username !== "string" ||
        typeof password !== "string" ||
        username.length < 1 ||
        username.length > 100 ||
        password.length < 1 ||
        password.length > 100
    ) {
        return res.status(400).json({ error: "Invalid input" });
    }

    const storedUser = process.env.MOD_USERNAME;
    const storedSalt = process.env.MOD_SALT;
    const storedHash = process.env.MOD_PASSWORD_HASH;

    const userBuf = Buffer.from(username, "utf8");
    const storedUserBuf = Buffer.from(storedUser, "utf8");
    if (
        userBuf.length !== storedUserBuf.length ||
        !crypto.timingSafeEqual(userBuf, storedUserBuf)
    ) {
        return res.json({ isMod: false });
    }

    const derivedKey = crypto.pbkdf2Sync(password, storedSalt, 100000, 64, "sha512");
    const storedHashBuf = Buffer.from(storedHash, "hex");
    if (
        derivedKey.length !== storedHashBuf.length ||
        !crypto.timingSafeEqual(derivedKey, storedHashBuf)
    ) {
        return res.json({ isMod: false });
    }

    res.json({ isMod: true });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
