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
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    if (!/^[A-Za-z0-9_]{1,30}$/.test(username)) {
        return res.status(400).send("Invalid username");
    }
    const escapeHtml = str => str.replace(/[&<>"'\/]/g, s => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
        "/": "&#x2F;"
    }[s]));
    const safeUsername = escapeHtml(username);
    res.setHeader("Content-Security-Policy", "default-src 'self'");
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
