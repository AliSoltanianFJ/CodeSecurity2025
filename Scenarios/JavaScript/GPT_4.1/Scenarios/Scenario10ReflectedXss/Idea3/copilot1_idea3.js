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
    // Input validation & sanitization
    const username = req.params.username;
    if (typeof username !== "string" || username.length === 0 || username.length > 30 || !/^[a-zA-Z0-9_\-]+$/.test(username)) {
        return res.status(400).send("Invalid username.");
    }

    // Prevent XSS by escaping output.
    const escapeHtml = str => str.replace(/[&<>"'`=\/]/g, s => (
        { "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;", "`":"&#96;", "=":"&#61;", "/":"&#47;" }[s]
    ));
    
    res.setHeader("Content-Security-Policy", "default-src 'self'");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("X-XSS-Protection", "1; mode=block");

    res.send(`<p>Hello, ${escapeHtml(username)}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
