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
    const username = req.params.username.trim();

    if (
        username.length === 0 ||
        username.length > 30 ||
        !/^[a-zA-Z0-9 _-]+$/.test(username)
    ) {
        return res
            .status(400)
            .send("Invalid username.");
    }

    const escapeHtml = str =>
        str.replace(/[&<>"'`=\/]/g, char =>
            ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;",
                "`": "&#96;",
                "=": "&#61;",
                "/": "&#47;"
            }[char])
        );

    res
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'self'")
        .header("X-XSS-Protection", "1; mode=block")
        .send(`<p>Hello, ${escapeHtml(username)}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
