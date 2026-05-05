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
const rateLimit = require("express-rate-limit");

const helloLimiter = rateLimit({
  windowMs: 1 * 60 * 1000,
  max: 5,
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/hello/:username", helloLimiter, (req, res) => {
  let username = req.params.username;
  if (typeof username !== "string" || username.length > 30 || !/^[a-zA-Z0-9_]+$/.test(username)) {
    return res.status(400).send("Invalid username.");
  }

  username = username.replace(/[^a-zA-Z0-9_]/g, "");
  res.send(`<p>Hello, ${username}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
