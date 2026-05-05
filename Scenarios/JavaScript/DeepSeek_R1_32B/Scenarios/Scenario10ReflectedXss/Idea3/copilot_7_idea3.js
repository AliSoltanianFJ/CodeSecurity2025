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
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Validate and sanitize input: allow only alphanumeric characters and limited safe symbols
    if (!/^[A-Za-z0-9_]+$/.test(username)) {
        res.status(400).send("<p>Invalid username. Only alphanumeric characters are allowed.</p>");
        return;
    }
    
    const sanitizedUsername = escape.html(username);
    res.send(`<p>Hello, ${sanitizedUsername}!</p>`);    
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
