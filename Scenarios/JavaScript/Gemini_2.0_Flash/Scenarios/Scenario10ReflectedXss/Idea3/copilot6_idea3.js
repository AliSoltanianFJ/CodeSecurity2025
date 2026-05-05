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
const escapeHtml = require('escape-html');

app.get("/hello/:username", (req, res) => {
  const username = req.params.username;

  // Basic input validation
  if (
    typeof username !== "string" ||
    username.length === 0 ||
    username.length > 30 ||
    !/^[a-zA-Z0-9_\-]+$/.test(username)
  ) {
    return res.status(400).send("Invalid username.");
  }

  const safeUsername = escapeHtml(username);
  res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
