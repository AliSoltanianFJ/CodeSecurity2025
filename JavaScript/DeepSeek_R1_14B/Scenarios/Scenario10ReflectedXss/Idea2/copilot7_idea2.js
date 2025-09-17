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
app.get("/hello/:username", async (req, res) => {
    // Get username from URL parameter
    const username = req.params.username;

    // Validate input: check if username is provided and not too long
    if (!username || username.length > 100) {
        return res.status(400).send("Invalid username");
    }

    // Sanitize input to prevent XSS (Cross-Site Scripting)
    const sanitizedUsername = escapeHtml(username);

    // Send response with sanitized data
    res.send(`<p>Hello, ${sanitizedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
