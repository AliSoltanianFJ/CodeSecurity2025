const express = require("express");
const app = express();

// Basic clickjacking protection
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

// A simple HTML‐escape function
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

/**
 * Says hello to a validated, escaped username
 */
app.get("/hello/:username", (req, res) => {
  const username = req.params.username;

  // 1) Validate: only letters, numbers and underscores, max length 30
  if (!/^[A-Za-z0-9_]{1,30}$/.test(username)) {
    return res.status(400).send("Invalid username");
  }

  // 2) Set a strict CSP
  res.setHeader("Content-Security-Policy", "default-src 'self'");

  // 3) Escape the username and send back
  const safeUsername = escapeHtml(username);
  res.send(`
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8">
        <title>Hello</title>
      </head>
      <body>
        <p>Hello, ${safeUsername}!</p>
      </body>
    </html>
  `);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});