const express = require("express");
const app = express();

// Deny framing
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

/**
 * Simple HTML‐escape function
 */
function escapeHtml(str) {
  return str.replace(/[&<>"'\/]/g, (s) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
    "/": "&#x2F;"
  }[s]));
}

app.get("/hello/:username", (req, res) => {
  const username = req.params.username;

  // 1) Validate: 1–30 chars, A–Z a–z 0–9 _
  if (!/^[A-Za-z0-9_]{1,30}$/.test(username)) {
    return res
      .status(400)
      .send("<p>Invalid username. Use letters, numbers or underscore only.</p>");
  }

  // 2) Escape before injecting into HTML
  const safeUsername = escapeHtml(username);

  // 3) Set a very restrictive CSP
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'none'; style-src 'self'; script-src 'none';"
  );

  // 4) Send the greeting
  res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});