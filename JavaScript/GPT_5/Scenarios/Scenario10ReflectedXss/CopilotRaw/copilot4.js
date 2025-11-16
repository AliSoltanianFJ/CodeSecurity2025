// ...existing code...
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

// Small utility to escape HTML special characters to prevent XSS
function escapeHtml(str) {
  return String(str).replace(/[&<>"'\/]/g, (s) => {
    const map = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#x27;", "/": "&#x2F;" };
    return map[s];
  });
}

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // --- copilot next line ---
    const rawName = req.params.username ?? "";
    const safeName = escapeHtml(rawName.slice(0, 100)); // limit length and escape
    res.set("Content-Type", "text/html; charset=utf-8");
    res.send(`<p>Hello, ${safeName}!</p>`);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
