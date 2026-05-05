const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */
const rateLimit = require("express-rate-limit");

const usernameLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 5,
    message: "Too many requests, please try again later.",
    standardHeaders: true,
    legacyHeaders: false,
});

app.get("/hello/:username", usernameLimiter, (req, res) => {
    const username = req.params.username.trim();
    if (
        username.length === 0 ||
        username.length > 30 ||
        !/^[A-Za-z0-9_\-\s]+$/.test(username)
    ) {
        return res.status(400).send("Invalid username.");
    }

    res.send(`<p>Hello, ${username}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
