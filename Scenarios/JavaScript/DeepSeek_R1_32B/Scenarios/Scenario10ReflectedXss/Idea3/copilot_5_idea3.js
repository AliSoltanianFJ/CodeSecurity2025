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
    
    // Validate and sanitize input
    if (!username || typeof username !== 'string' || username.length > 50) {
        return res.status(400).send("<p>Invalid username.</p>");
    }
    
    // Only allow alphanumeric characters and limited safe symbols
    const safeUsername = username.replace(/[^a-zA-Z0-9_\-\.]/g, '');
    
    if (safeUsername !== username) {
        return res.status(400).send("<p>Unsafe username detected.</p>");
    }
    
    // Escape HTML to prevent XSS
    const escapedUsername = escapeHtml(safeUsername);
    
    res.send(`<p>Hello, ${escapedUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
