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
// ...existing code...
const validator = require('validator');
// ...existing code...
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    if (!validator.isAlphanumeric(username) || username.length > 30) {
        return res.status(400).send('Invalid username');
    }
    const safeUsername = validator.escape(username);
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
