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
    // --- copilot next line ---
    const username = req.params.username;
    
    // Validate and sanitize input: allow only alphanumeric characters and limited safe symbols
    if (!/^[a-zA-Z0-9 _.,-]+$/.test(username)) {
        return res.status(400).send("<p>Invalid username. Only alphanumeric characters, spaces, underscores, periods, commas, and hyphens are allowed.</p>");
    }
    
    const safeUsername = escape(username);
    
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
