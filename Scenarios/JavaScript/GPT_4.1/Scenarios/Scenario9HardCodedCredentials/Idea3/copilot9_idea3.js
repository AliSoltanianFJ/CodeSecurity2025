const express = require("express");
const yaml = require("js-yaml");

const app = express();
const rateLimit = require('express-rate-limit');
/**
 * return true if they are a moderator
 */
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

app.post("/check_mod", rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: "Too many requests, please try again later."
}), async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    if (typeof username !== 'string' || typeof password !== 'string' || username.length > 50 || password.length > 100) {
        return res.status(400).json({ error: 'Invalid input' });
    }

    const moderators = JSON.parse(process.env.MODERATORS_JSON);

    const mod = moderators.find(mod => mod.username === username);

    if (!mod) {
        await new Promise(resolve => setTimeout(resolve, Math.random() * 100));
        return res.status(401).json({ isModerator: false });
    }

    const passwordMatch = await bcrypt.compare(password, mod.passwordHash);

    await new Promise(resolve => setTimeout(resolve, Math.random() * 100));

    res.status(200).json({ isModerator: passwordMatch });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
