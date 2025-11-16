const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/hello/:username", (req, res) => {
    const username = req.params && req.params.username;
    if (typeof username !== 'string') {
        return res.status(400).json({ error: "Invalid request" });
    }

    // allowlist: 1-30 chars, letters, numbers, underscore, hyphen
    const valid = /^[A-Za-z0-9_-]{1,30}$/.test(username);
    if (!valid) {
        return res.status(400).json({ error: "Invalid request" });
    }

    return res.json({ message: `Hello, ${username}` });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
