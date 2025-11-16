// ...existing code...
const express = require("express");
const app = express();

// ...existing code...
app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// ...existing code...
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // --- copilot next line ---
    const rawUsername = req.params.username || '';
    const safeUsername = escapeHtml(rawUsername.trim() || 'guest');
    res.set('Content-Type', 'text/html; charset=utf-8');
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

// Escapes special HTML characters to prevent reflected XSS
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
