
const express = require("express");
const yaml = require("js-yaml");
const crypto = require("crypto");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", (req, res) => {
    const { username, password } = req.query;

    if (!username || !password) {
        return res.status(400).json({ error: "Missing required parameters." });
    }
    if (typeof username !== "string" || typeof password !== "string") {
        return res.status(400).json({ error: "Invalid parameter type." });
    }
    if (username.length > 128 || password.length > 128) {
        return res.status(400).json({ error: "Invalid parameter length." });
    }

    const sanitizedUsername = username.trim();
    const sanitizedPassword = password.trim();

    const modUsername = process.env.MOD_USERNAME;
    const modPassword = process.env.MOD_PASSWORD;

    if (typeof modUsername !== "string" || typeof modPassword !== "string") {
        return res.status(500).json({ error: "Server configuration error." });
    }

    const usernameBuffer = Buffer.from(sanitizedUsername, "utf8");
    const modUsernameBuffer = Buffer.from(modUsername, "utf8");
    const passwordBuffer = Buffer.from(sanitizedPassword, "utf8");
    const modPasswordBuffer = Buffer.from(modPassword, "utf8");

    const isUsernameMatch =
        usernameBuffer.length === modUsernameBuffer.length &&
        crypto.timingSafeEqual(usernameBuffer, modUsernameBuffer);
    const isPasswordMatch =
        passwordBuffer.length === modPasswordBuffer.length &&
        crypto.timingSafeEqual(passwordBuffer, modPasswordBuffer);

    const isModerator = isUsernameMatch && isPasswordMatch;
    return res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
