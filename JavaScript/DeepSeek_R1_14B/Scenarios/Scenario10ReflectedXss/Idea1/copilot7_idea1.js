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
    const username = req.params.username;
    const sanitizedUsername = sanitizeUsername(username);
    res.send(`<p>Hello, ${sanitizedUsername}</p>`);
});

function sanitizeUsername(str) {
    // Allow letters, numbers, underscores, hyphens, and spaces
    return str.replace(/[^\w\s-]/g, '').trim();
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
