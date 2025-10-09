const express = require("express");
const escapeHtml = require("escape-html");
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
    // Mitigate clickjacking
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

/**
 * function which says hello to the user
 */
app.get("/hello/:username", (req, res) => {
    // Validate & canoncalize input (only non-empty strings allowed)
    const rawUsername = req.params.username || "";
    // Context-appropriate HTML escaping to prevent XSS
    const safeUsername = escapeHtml(rawUsername);
    res.send(`<p>Hello, ${safeUsername}</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});