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

const rateLimitMiddleware = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute window
  max: 5, // limit each IP to 5 requests per windowMs
  message: "Too many requests, please try again later.",
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/hello/:username", rateLimitMiddleware, (req, res) => {
  const username = req.params.username;

  // Input validation: allow only alphanumeric and underscores
  if (!/^[a-zA-Z0-9_]+$/.test(username)) {
    return res.status(400).send("Invalid username.");
  }

  res.send(`Hello, ${username}!`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
