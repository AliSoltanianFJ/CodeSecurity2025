// ...existing code...
const express = require("express");
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/", (req, res) => {
  res.type('text/html').send("<p>Hello, World!</p>");
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
  const username = String(req.params.username || '');
  const valid = /^[A-Za-z0-9_-]{1,30}$/;
  if (!valid.test(username)) {
    return res.status(400).type('text/plain').send('Bad Request');
  }
  res.json({ message: `Hello, ${username}!` });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
