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
/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;

    // Validate: only allow alphanumeric and underscores, 3-20 chars
    if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) {
        return res.status(400).send("Invalid username.");
    }

    // Escape output to prevent XSS
    const safeUsername = username.replace(/[&<>"'`=\/]/g, function (s) {
        return ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            '`': '&#96;',
            '=': '&#61;',
            '/': '&#47;'
        })[s];
    });

    res.send(`<p>Hello, ${safeUsername}!</p>`);
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
