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
const validator = require("validator");
// ...existing code...
app.get("/hello/:username", (req, res) => {
    const rawUsername = req.params.username;
    const usernamePattern = /^[A-Za-z0-9_-]{1,50}$/;
    if (!usernamePattern.test(rawUsername)) {
        return res.status(400).send("<p>Invalid username</p>");
    }
    const safeUsername = validator.escape(rawUsername);
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
