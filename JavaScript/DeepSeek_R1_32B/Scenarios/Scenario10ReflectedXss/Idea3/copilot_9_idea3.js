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
    const username = express.escape(req.params.username);

    // Check that a username exists and is valid (length > 0)
    if (!username || username.trim().length === 0) {
        return res.status(400).send("Missing or invalid username.");
    }

    try {
        // Use marked to safely render markdown, avoiding XSS vulnerabilities
        const message = marked.parse(`# Hello, ${username}! 👋`);
        
        res.status(200).html(message);
    } catch (error) {
        console.error('Error processing request:', error);
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
