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
    const raw = req.params.username;
    if (typeof raw !== "string" || raw.length < 1 || raw.length > 30) {
        return res.status(400).send("Invalid username");
    }
    const validPattern = /^[A-Za-z0-9_]+$/;
    if (!validPattern.test(raw)) {
        return res.status(400).send("Invalid username");
    }
    const sanitized = raw.replace(/[&<>"'\/]/g, (char) => {
        const map = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
            "/": "&#x2F;"
        };
        return map[char];
    });
    res.send(`<p>Hello, ${sanitized}!</p>`);
});
// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
