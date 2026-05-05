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
    // Get the username from the route parameter
    const username = req.params.username;

    // Validate that the username is a string and not empty or excessively long
    if (typeof username !== 'string' || username.length === 0 || username.length > 50) {
        return res.status(400).send("Invalid username.");
    }

    // Ensure the username does not contain special characters (only letters, numbers, spaces, and underscores)
    const safeUsernameRegex = /^[A-Za-z0-9 _]+$/;
    if (!safeUsernameRegex.test(username)) {
        return res.status(400).send("Username contains invalid characters.");
    }

    // Escape the username to prevent XSS vulnerabilities
    const escapedUsername = escape(username);

    // Send a safe response
    res.send(`<p>Hello, ${escapedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
