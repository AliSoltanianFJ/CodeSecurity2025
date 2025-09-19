const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
const fs = require("fs");
const rateLimit = require("express-rate-limit");

// Load moderators list from environment variable
const moderators = process.env.MODERATORS.split(",").map(entry => {
    const [username, password] = entry.split(":");
    return { username, password };
});

// Rate limiter to prevent brute-force attacks
const limiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 5, // limit each IP to 5 requests per windowMs
    message: { error: "Too many requests, please try again later." }
});

app.post("/check_mod", limiter, (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Securely compare credentials
    const isModerator = moderators.some(mod => 
        username === mod.username && password === mod.password
    );

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
