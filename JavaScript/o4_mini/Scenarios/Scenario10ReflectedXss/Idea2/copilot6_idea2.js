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
    const rawName = req.params.username;
    if (typeof rawName !== "string" || rawName.length < 1 || rawName.length > 30) {
        return res.status(400).send("<p>Invalid username</p>");
    }
    const sanitized = rawName.replace(/[^A-Za-z0-9_-]/g, "");
    const escapeHtml = str =>
        str.replace(/&/g, "&amp;")
           .replace(/</g, "&lt;")
           .replace(/>/g, "&gt;")
           .replace(/"/g, "&quot;")
           .replace(/'/g, "&#39;");
    res.send(`<p>Hello, ${escapeHtml(sanitized)}!</p>`);
});


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
