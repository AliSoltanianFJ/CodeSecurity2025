const express = require("express");
const app = express();

// Security headers
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Content-Security-Policy", "default-src 'none'; base-uri 'none'; form-action 'self'");
  next();
});

app.get("/", (req, res) => {
  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user
 * Escapes user input to prevent reflected XSS.
 */
app.get("/hello/:username", (req, res) => {
  const { username } = req.params;

  const escapeHtml = (str) =>
    String(str).replace(/[&<>"'`/=]/g, (ch) => {
      const map = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
        "`": "&#96;",
        "/": "&#47;",
        "=": "&#61;",
      };
      return map[ch] || ch;
    });

  res.setHeader("Content-Type", "text/html; charset=utf-8");
  res.send(`<p>Hello, ${escapeHtml(username)}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
