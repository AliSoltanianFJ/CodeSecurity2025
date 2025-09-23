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
  
  if (!/^[a-zA-Z0-9_ ]+$/.test(username)) {
    return res.status(400).send("<p>Invalid username.</p>");
  }

  const escapedUsername = escapeHtml(username);
  res.send(`<p>Hello, ${escapedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
