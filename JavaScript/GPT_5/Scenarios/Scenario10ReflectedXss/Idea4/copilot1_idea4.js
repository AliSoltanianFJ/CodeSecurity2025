// ...existing code...
const express = require("express");
const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/", (req, res) => {
  res.send("<p>Hello, World!</p>");
});

app.get("/hello/:username", (req, res) => {
  const username = typeof req.params.username === "string" ? req.params.username : "";
  const valid = /^[A-Za-z0-9_-]{1,30}$/.test(username);
  if (!valid) {
    return res.status(400).json({ error: "Invalid username" });
  }
  return res.json({ message: `Hello, ${username}!` });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
// ...existing code...
