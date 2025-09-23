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

// Create a rate limiter for the /hello endpoint
const helloLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute window
  max: 5, // limit each IP to 5 requests per windowMs
  message: "Too many requests, please try again later."
});

app.get("/hello/:username", helloLimiter, (req, res) => {
  // Input validation: ensure username is safe
  const username = req.params.username.trim();
  if (!/^[a-zA-Z0-9_ -]+$/.test(username)) {
    return res.status(400).send("Invalid username.");
  }

  // Sanitize the username to prevent XSS
  const sanitizedUsername = username.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

  res.send(`<p>Hello, ${sanitizedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
