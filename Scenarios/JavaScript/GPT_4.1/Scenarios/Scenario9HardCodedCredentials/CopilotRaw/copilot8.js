const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});


// ...existing code...
/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // Hardcoded moderator credentials
    const moderators = [
        { username: "admin", password: "supersecret" },
        { username: "mod1", password: "modpass1" }
    ];

    const isModerator = moderators.some(
        (mod) => mod.username === username && mod.password === password
    );

    res.json({ isModerator });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
