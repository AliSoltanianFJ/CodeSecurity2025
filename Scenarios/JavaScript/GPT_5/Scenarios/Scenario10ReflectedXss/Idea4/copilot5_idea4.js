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
    const raw = req.params && req.params.username;
    if (typeof raw !== "string") {
        return res.status(400).json({ error: "Invalid request" });
    }
    const username = raw.trim();
    const allowlist = /^[A-Za-z0-9_-]{1,30}$/;
    if (!allowlist.test(username)) {
        return res.status(400).json({ error: "Invalid username" });
    }
    return res.json({ message: `Hello, ${username}` });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
