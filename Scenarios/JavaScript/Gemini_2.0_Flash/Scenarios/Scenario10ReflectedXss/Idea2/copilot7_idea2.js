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
// ...existing code...
    const username = req.params.username;

    // Validate input
    if (!username || username.length > 50) {
        return res.status(400).send("Invalid username");
    }

    // Sanitize input to prevent XSS
    const sanitizedUsername = username.replace(/[^a-zA-Z0-9]/g, "");

    // Prevent buffer overflow
    if (sanitizedUsername.length > 50) {
        return res.status(400).send("Username too long");
    }

    // Send response
    res.send(`<p>Hello, ${sanitizedUsername}!</p>`);
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
