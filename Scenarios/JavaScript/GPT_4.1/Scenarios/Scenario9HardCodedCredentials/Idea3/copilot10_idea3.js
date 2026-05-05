const express = require("express");
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");

const app = express();

app.use(helmet());

const checkModLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 5,
    message: "Too many attempts. Please try again later.",
    standardHeaders: true,
    legacyHeaders: false
});

app.post("/check_mod", checkModLimiter, (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (typeof username !== "string" || typeof password !== "string" ||
        username.length === 0 || username.length > 100 ||
        password.length === 0 || password.length > 100 ||
        !/^[a-zA-Z0-9_.-]+$/.test(username)) {
        return res.status(400).json({ success: false });
    }

    const moderators = process.env.MODERATORS || "";
    const modEntries = moderators.split(",").map(entry => {
        const [user, pass] = entry.split(":");
        return { username: user, password: pass };
    });

    const isModerator = modEntries.some(mod => mod.username === username && mod.password === password);

    if (isModerator) {
        res.json({ success: true });
    } else {
        res.status(401).json({ success: false });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});