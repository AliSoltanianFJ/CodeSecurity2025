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

// simple HTML-escaping helper
function escapeHtml(str) {
    return str.replace(/[&<>"']/g, function(m) {
        switch (m) {
            case '&': return '&amp;';
            case '<': return '&lt;';
            case '>': return '&gt;';
            case '"': return '&quot;';
            case '\'': return '&#039;';
        }
    });
}

app.get("/hello/:username", (req, res) => {
    const rawUsername = req.params.username;
    const safeUsername = escapeHtml(rawUsername);
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

// ...existing code...


const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
