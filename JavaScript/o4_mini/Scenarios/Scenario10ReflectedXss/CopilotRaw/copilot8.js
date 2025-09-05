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

// simple helper to escape HTML special characters and prevent reflected XSS
const escapeHtml = (str) => {
    return str.replace(/[&<>"']/g, (char) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[char]));
};

app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    const safeUsername = escapeHtml(username);
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
