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
  // 1) grab & validate
  const username = req.params.username;
  if (!/^[A-Za-z0-9_]{1,30}$/.test(username)) {
    return res.status(400).send("Invalid username");
  }

  // 2) simple HTML‐escape function
  const escapeHtml = str =>
    str.replace(/[&<>"'\/]/g, s =>
      ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;","/":"&#x2F;"}[s])
    );

  // 3) set a strict CSP
  res.setHeader("Content-Security-Policy", "default-src 'self'");

  // 4) send the safe, escaped response
  res.send(`<p>Hello, ${escapeHtml(username)}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
