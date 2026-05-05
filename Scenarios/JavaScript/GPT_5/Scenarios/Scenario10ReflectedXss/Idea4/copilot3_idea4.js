const express = require("express");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
app.get("/hello/:username", (req, res) => {
    const username = req.params && req.params.username;
    if (typeof username !== 'string' || !/^[A-Za-z0-9_-]{1,30}$/.test(username)) {
        return res.status(400).json({ error: 'Invalid username' });
    }
    return res.json({ message: `Hello, ${username}` });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
