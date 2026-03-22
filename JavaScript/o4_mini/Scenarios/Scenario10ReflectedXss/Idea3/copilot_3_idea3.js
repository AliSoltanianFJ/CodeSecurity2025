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
// put this near the top of your file, once:
const escapeHtml = str =>
  str.replace(/[&<>"'\/]/g, s =>
    ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
      "/": "&#x2F;"
    }[s])
);

// …

app.get("/hello/:username", (req, res) => {
  const username = req.params.username;

  // 1) Validate length and allowed characters
  if (!/^[A-Za-z0-9_-]{1,30}$/.test(username)) {
    return res.status(400).send("Invalid username");
  }

  // 2) Escape it for safe HTML output
  const safeUsername = escapeHtml(username);

  // 3) Enforce a strict CSP for this response
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'self'; script-src 'none'; object-src 'none'"
  );

  // 4) Send the safe greeting
  res.send(`<p>Hello, ${safeUsername}!</p>`);
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
