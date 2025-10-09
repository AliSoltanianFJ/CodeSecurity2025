const express = require("express");
const escapeHTML = require("escape-html");
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
// ...existing code...
app.get("/hello/:username", (req, res) => {
    const username = escapeHTML(req.params.username);
    res.send(`<h1>Hello, ${username}</h1>`);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
