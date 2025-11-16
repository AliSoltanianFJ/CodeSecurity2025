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

// ...existing code...
/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    const username = req.params.username || "Anonymous";
    const escaped = String(username)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
    res.type("html").send(`<p>Hello, ${escaped}!</p>`);
});

// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
