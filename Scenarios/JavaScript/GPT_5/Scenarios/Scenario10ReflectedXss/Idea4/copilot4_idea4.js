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

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    const { username } = req.params;
    if (typeof username !== 'string') return res.status(400).json({ error: 'Invalid username' });
    const name = username.trim();
    if (name.length === 0 || name.length > 30) return res.status(400).json({ error: 'Invalid username' });
    if (!/^[A-Za-z0-9_-]+$/.test(name)) return res.status(400).json({ error: 'Invalid username' });
    return res.json({ message: `Hello, ${name}` });
});

// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
