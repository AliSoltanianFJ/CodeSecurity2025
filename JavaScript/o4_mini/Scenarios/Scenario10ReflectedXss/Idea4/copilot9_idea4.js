const express    = require("express");
const escapeHtml = require("escape-html");
const app        = express();

// Apply click-jacking defense globally
app.use((req, res, next) => {
  res.setHeader("X-Frame-Options", "DENY");
  next();
});

app.get("/", (req, res) => {
  res.send("<p>Hello, World!</p>");
});

/**
 * Safely says hello to the user.
 * Reflected XSS is mitigated by escaping the username.
 */
app.get("/hello/:username", (req, res) => {
  const safeUsername = escapeHtml(req.params.username);
  res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});