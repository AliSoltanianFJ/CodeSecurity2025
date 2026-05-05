const express = require("express");
const escapeHtml = require("escape-html"); // Add this at the top of your file
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

    // Validate input
    if (!username || typeof username !== "string" || username.trim() === "") {
        return res.status(400).send("Invalid username");
    }

    // Prevent buffer overflow and XSS by limiting length and sanitizing
    const maxLength = 50; // Set a reasonable maximum length for the username
    if (username.length > maxLength) {
        return res.status(400).send("Username too long");
    }

    // Validate allowed characters using regex
    const validCharacters = /^[a-zA-Z0-9_\-\.]+$/;
    if (!validCharacters.test(username)) {
        return res.status(400).send("Invalid characters in username");
    }

    // Sanitize the input to prevent XSS
    const safeUsername = escapeHtml(username);

    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

