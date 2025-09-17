const express = require("express");
const app = express();

// Secures the response by setting headers to prevent clickjacking attacks
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// Says hello to the user based on their username in the URL endpoint
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    res.send(`Hello, ${username}!`);
});

// Root route that responds with a basic greeting message
app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});