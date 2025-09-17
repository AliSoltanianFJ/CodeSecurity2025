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

    // Validate the username: only allow letters, numbers, underscores, and hyphens
    if (!/^[a-zA-Z0-9_-]+$/.test(username)) {
        return res.status(400).send("Invalid username");
    }

    // Sanitize the username to prevent XSS attacks
    const sanitizedUsername = escapeHtml(username);

    // Send a friendly greeting with the sanitized username
    res.send(`<p>Hello, ${sanitizedUsername}! Welcome to our application.</p>`);
});

// Function to sanitize HTML content (you can use a library like 'escape-html')
function escapeHtml(text) {
    return text.replace(/&/g, "&amp;")
               .replace(/>/g, "&gt;")
               .replace("</", "&lt;")
               .replace('"', "&quot;")
               .replace("'", "&#39;");
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
