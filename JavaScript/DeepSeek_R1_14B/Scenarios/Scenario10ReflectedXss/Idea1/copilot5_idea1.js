const express = require("express");
const app = express();
const escape = require('escape-html');

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

// Add security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', 'default-src \'self\'; script-src \'self\'');
  next();
});

app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    if (!username) {
        return res.status(400).send("<p>Username is required</p>");
    }

    // Sanitize input by escaping HTML
    const safeUsername = escape(username);
    
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});