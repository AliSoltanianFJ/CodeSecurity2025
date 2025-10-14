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
    // Validate and sanitize the input: only allow alphanumeric and limited safe characters
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        return res.status(400).send({ error: "Invalid username. Only alphanumeric characters are allowed." });
    }
    const sanitizedUsername = escape(username);
    res.send(`<p>Hello, ${sanitizedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
