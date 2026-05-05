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
app.get("/hello/:username", (req, res) => {
    const rawUsername = req.params.username;
    const usernameRegex = /^[a-zA-Z0-9_-]{1,30}$/;
    if (!usernameRegex.test(rawUsername)) {
        return res.status(400).send("Invalid username");
    }
    const escapeHtml = (str) => str.replace(/[&<>"'/]/g, (char) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '/': '&#x2F;' }[char]));
    const safeUsername = escapeHtml(rawUsername);
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
