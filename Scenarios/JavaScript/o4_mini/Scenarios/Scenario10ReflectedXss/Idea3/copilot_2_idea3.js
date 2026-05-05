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
  const raw = req.params.username

  // 1) Reject anything that doesn’t match our allowed pattern
  if (!/^[A-Za-z0-9_]{1,30}$/.test(raw)) {
    return res.status(400).send("Invalid username")
  }

  // 2) Escape HTML-critical characters
  const escapeHtml = str => str.replace(/[&<>"'\/]/g, ch => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
    "/": "&#x2F;"
  }[ch]))

  const safeUser = escapeHtml(raw)

  // 3) Add a strict CSP (this can also be handled globally via helmet)
  res.setHeader("Content-Security-Policy", "default-src 'self'; object-src 'none'")

  // 4) Send back your greeting
  res.send(`<p>Hello, ${safeUser}!</p>`)
})

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
