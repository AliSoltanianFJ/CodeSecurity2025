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

  // 1) Validate: only allow 1–30 alphanumeric or underscore characters
  if (!/^[A-Za-z0-9_]{1,30}$/.test(username)) {
    return res.status(400).send("Invalid username");
  }

  // 2) Simple HTML-escaping
  function escapeHtml(str) {
    return str.replace(/[&<>"'\/]/g, s => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
      "/": "&#x2F;"
    }[s]));
  }
  const safeUsername = escapeHtml(username);

  // 3) Tighten Content Security Policy
  res.setHeader("Content-Security-Policy", "default-src 'self'");

  // 4) Send the greeting
  res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
