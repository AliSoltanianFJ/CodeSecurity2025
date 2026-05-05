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
    const username = req.params && req.params.username;
    if (typeof username !== "string") {
        return res.status(400).type("text/plain").send("Bad Request");
    }

    if (!/^[A-Za-z0-9_-]{1,30}$/.test(username)) {
        return res.status(400).type("text/plain").send("Bad Request");
    }

    const escapeHtml = (str) =>
        String(str).replace(/[&<>"'\/]/g, (s) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
            "/": "&#x2F;"
        }[s]));

    const safe = escapeHtml(username);
    res.type("text/html").send(`<p>Hello, ${safe}</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
