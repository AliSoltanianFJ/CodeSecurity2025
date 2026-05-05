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
    // Validate and sanitize the username: allow only alphanumeric characters and enforce a maximum length.
    if (!/^[A-Za-z0-9]+$/.test(username) || username.length > 20) {
        return res.status(400).send("Invalid username. Only alphanumeric characters are allowed, up to 20 characters.");
    }
    const safeUsername = escape(username); // Escape HTML special characters
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
